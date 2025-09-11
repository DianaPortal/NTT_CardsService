package com.nttdata.cards_service.service.impl;

import com.nttdata.cards_service.api.mapper.CardMapper;
import com.nttdata.cards_service.integration.credits.CreditsClient;
import com.nttdata.cards_service.integration.credits.dto.OverdueRes;
import com.nttdata.cards_service.model.*;
import com.nttdata.cards_service.model.entity.Card;
import com.nttdata.cards_service.model.value.StoredOperation;
import com.nttdata.cards_service.repository.CardRepository;
import com.nttdata.cards_service.service.CardService;
import com.nttdata.cards_service.service.MovementQueryService;
import com.nttdata.cards_service.service.PayCreditService;
import com.nttdata.cards_service.service.PrimaryBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.nttdata.cards_service.service.DebitOrchestratorService;
import com.nttdata.cards_service.integration.accounts.dto.AccountDto;
import com.nttdata.cards_service.cache.CardsCacheService;
import java.time.OffsetDateTime;
import java.util.*;
import static com.nttdata.cards_service.service.CardDomainUtils.normalizeAccounts;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository repo;
    private final CreditsClient credits;
    private final DebitOrchestratorService debit;
    private final PayCreditService payCredit;
    private final MovementQueryService movements;
    private final PrimaryBalanceService primaryBalance;
    private final CardMapper mapper;
    private final CardsCacheService cache;
    private static final String DEBIT_CARD_TYPE = "DEBIT";
    private static final String NOT_A_DEBIT_CARD_MSG = "Not a DEBIT card";
    private final com.nttdata.cards_service.integration.accounts.AccountsClient accountsClient;

    @Override
    public Mono<CardResponse> createCard(CardRequest request) {
        return credits.hasOverdue(request.getCustomerId())
            .map(OverdueRes::isHasOverdue).defaultIfEmpty(false)
            .flatMap(has -> {
                if (Boolean.TRUE.equals(has)) return Mono.error(new IllegalStateException("Client has overdue debts"));

                Card entity = mapper.toEntity(request);

                if (DEBIT_CARD_TYPE.equals(entity.getCardType())) {
                    if (entity.getPrimaryAccountId() == null)
                        return Mono.error(new IllegalStateException("DEBIT card requires primaryAccountId"));

                    // validar cuenta principal: existe y activa
                    return accountsClient.getAccount(entity.getPrimaryAccountId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Primary account not found")))
                        .flatMap(acc -> {
                            if (Boolean.FALSE.equals(safeActive(acc)))
                                return Mono.error(new IllegalStateException("Primary account is not active"));
                            entity.setAccounts(normalizeAccounts(entity.getPrimaryAccountId(), entity.getAccounts()));
                            if (entity.getCreationDate() == null) entity.setCreationDate(OffsetDateTime.now());
                            entity.setUpdatedDate(OffsetDateTime.now());
                            return repo.save(entity).map(mapper::toResponse);
                        });
                }

                // CREDIT
                if ("CREDIT".equals(entity.getCardType()) && entity.getCreditId() == null)
                    return Mono.error(new IllegalStateException("CREDIT card requires creditId"));

                if (entity.getCreationDate() == null) entity.setCreationDate(OffsetDateTime.now());
                entity.setUpdatedDate(OffsetDateTime.now());
                return repo.save(entity).map(mapper::toResponse);
            });
    }

    private boolean safeActive(AccountDto a) {
        return a.getActive() == null || Boolean.TRUE.equals(a.getActive());
    }

    @Override
    public Flux<CardResponse> listCards() {
        return repo.findAll().map(mapper::toResponse);
    }

    @Override
    public Mono<CardResponse> getCardById(String id) {
        return cache.cardById(id, () ->
            repo.findById(id)
                .map(mapper::toResponse));
    }

    @Override
    public Mono<CardResponse> updateCard(String id, CardRequest request) {
        return repo.findById(id)
            .flatMap(db -> {
                Card upd = mapper.toEntity(request);
                upd.setId(db.getId());
                upd.setCreationDate(db.getCreationDate());
                upd.setUpdatedDate(OffsetDateTime.now());
                // Si es DEBIT y cambia principal, validar
                if (DEBIT_CARD_TYPE.equals(upd.getCardType()) && upd.getPrimaryAccountId() != null) {
                    return accountsClient.getAccount(upd.getPrimaryAccountId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Primary account not found")))
                        .flatMap(acc -> {
                            if (Boolean.FALSE.equals(safeActive(acc)))
                                return Mono.error(new IllegalStateException("Primary account is not active"));
                            upd.setAccounts(normalizeAccounts(upd.getPrimaryAccountId(), upd.getAccounts()));
                            return repo.save(upd).map(mapper::toResponse);
                        });
                }
                return repo.save(upd).map(mapper::toResponse);
            });
    }


    @Override
    public Mono<Void> deleteCard(String id) {
        return repo.deleteById(id);
    }

    @Override
    public Mono<CardResponse> replaceAccounts(String id, CardAccountsReplaceRequest body) {
        return repo.findById(id).flatMap(c -> {
            if (!DEBIT_CARD_TYPE.equals(c.getCardType())) return Mono.error(new IllegalStateException(NOT_A_DEBIT_CARD_MSG));
            List<String> accountsList = body.getAccounts();
            if (accountsList == null || accountsList.isEmpty())
                return Mono.error(new IllegalStateException("accounts required"));
            if (!accountsList.contains(body.getPrimaryAccountId()))
                return Mono.error(new IllegalStateException("primaryAccountId must be included"));

            // validar que TODAS existen y están activas (si el dato viene)
            return Flux.fromIterable(accountsList)
                .flatMap(accountsClient::getAccount)
                .collectList()
                .flatMap(__ -> {
                    c.setPrimaryAccountId(body.getPrimaryAccountId());
                    c.setAccounts(normalizeAccounts(body.getPrimaryAccountId(), accountsList));
                    c.setUpdatedDate(OffsetDateTime.now());
                    return repo.save(c).map(mapper::toResponse);
                });
        });
    }

    @Override
    public Mono<CardResponse> addAccount(String id, AddAccountRequest body) {
        return repo.findById(id).flatMap(c -> {
            if (!DEBIT_CARD_TYPE.equals(c.getCardType())) return Mono.error(new IllegalStateException(NOT_A_DEBIT_CARD_MSG));
            return accountsClient.getAccount(body.getAccountId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Account not found")))
                .flatMap(acc -> {
                    if (Boolean.FALSE.equals(safeActive(acc)))
                        return Mono.error(new IllegalStateException("Account is not active"));
                    if (!c.getAccounts().contains(body.getAccountId()))
                        c.getAccounts().add(body.getAccountId());
                    c.setAccounts(normalizeAccounts(c.getPrimaryAccountId(), c.getAccounts()));
                    c.setUpdatedDate(OffsetDateTime.now());
                    return repo.save(c).map(mapper::toResponse);
                });
        });
    }

    @Override
    public Mono<CardResponse> removeAccount(String id, String accountId) {
        return repo.findById(id).flatMap(c -> {
            if (!DEBIT_CARD_TYPE.equals(c.getCardType())) return Mono.error(new IllegalStateException(NOT_A_DEBIT_CARD_MSG));
            if (accountId.equals(c.getPrimaryAccountId()))
                return Mono.error(new IllegalStateException("Cannot remove primary account"));
            c.getAccounts().removeIf(a -> a.equals(accountId));
            c.setUpdatedDate(OffsetDateTime.now());
            return repo.save(c).map(mapper::toResponse);
        });
    }

    @Override
    public Mono<CardResponse> reorderAccounts(String id, AccountReorderRequest body) {
        return repo.findById(id).flatMap(c -> {
            if (!DEBIT_CARD_TYPE.equals(c.getCardType())) return Mono.error(new IllegalStateException(NOT_A_DEBIT_CARD_MSG));
            List<String> ids = body.getAccountIds();
            if (ids == null || ids.isEmpty())
                return Mono.error(new IllegalStateException("accountIds required"));
            if (!ids.contains(c.getPrimaryAccountId()))
                return Mono.error(new IllegalStateException("primary account must be included"));
            c.setAccounts(normalizeAccounts(c.getPrimaryAccountId(), ids));
            c.setUpdatedDate(OffsetDateTime.now());
            return repo.save(c).map(mapper::toResponse);
        });
    }

    @Override
    public Mono<CardResponse> setPrimaryAccount(String id, SetPrimaryAccountRequest body) {
        return repo.findById(id).flatMap(c -> {
            if (!DEBIT_CARD_TYPE.equals(c.getCardType())) return Mono.error(new IllegalStateException(NOT_A_DEBIT_CARD_MSG));
            if (!c.getAccounts().contains(body.getAccountId()))
                return Mono.error(new IllegalStateException("Account not associated to the card"));
            return accountsClient.getAccount(body.getAccountId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Account not found")))
                .flatMap(acc -> {
                    if (Boolean.FALSE.equals(safeActive(acc)))
                        return Mono.error(new IllegalStateException("Account is not active"));
                    c.setPrimaryAccountId(body.getAccountId());
                    c.setAccounts(normalizeAccounts(body.getAccountId(), c.getAccounts()));
                    c.setUpdatedDate(OffsetDateTime.now());
                    return repo.save(c).map(mapper::toResponse);
                });
        });
    }

    @Override
    public Mono<CardOperationResponse> debitPayment(String id, DebitPaymentRequest req) {
        return debit.debit(id, req.getOperationId(), req.getAmount(),
                        "DEBIT_PAYMENT", java.util.Map.of("channel", req.getChannel(), "merchant", req.getMerchant()), "purchase")
                .map(StoredOperation::getResult);
    }

    @Override
    public Mono<CardOperationResponse> debitWithdrawal(String id, DebitWithdrawalRequest req) {
        return debit.debit(id, req.getOperationId(), req.getAmount(),
                        "DEBIT_WITHDRAWAL", java.util.Map.of("channel", req.getChannel(), "atmId", req.getAtmId()), "withdrawal")
                .map(StoredOperation::getResult);
    }

    @Override
    public Mono<CardOperationResponse> payCreditWithDebitCard(String id, PayCreditRequest req) {
        return payCredit.pay(id, req.getOperationId(), req.getCreditId(), req.getAmount(), req.getNote())
                .map(StoredOperation::getResult);
    }

    @Override
    public Flux<CardMovement> lastMovements(String id, Integer limit) {
        int n = (limit == null || limit < 1) ? 10 : Math.min(limit, 100);
        return movements.lastMovements(id, n);
    }

    @Override
    public Mono<PrimaryAccountBalance> primaryBalance(String id) {
        return primaryBalance.get(id);
    }
}