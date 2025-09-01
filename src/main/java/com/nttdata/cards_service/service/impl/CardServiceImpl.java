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

import java.time.OffsetDateTime;

import static com.nttdata.cards_service.service.CardDomainUtils.normalizeAccounts;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository repo;
    private final CreditsClient credits;
    private final PayCreditService.DebitOrchestratorService debit;
    private final PayCreditService payCredit;
    private final MovementQueryService movements;
    private final PrimaryBalanceService primaryBalance;
    private final CardMapper mapper;

    @Override
    public Mono<CardResponse> createCard(CardRequest request) {
        return credits.hasOverdue(request.getCustomerId())
                .map(OverdueRes::isHasOverdue)
                .defaultIfEmpty(false)
                .flatMap(has -> {
                    if (has) return Mono.error(new IllegalStateException("Client has overdue debts"));

                    Card entity = mapper.toEntity(request);
                    if ("DEBIT".equals(entity.getCardType()) && entity.getPrimaryAccountId() == null)
                        return Mono.error(new IllegalStateException("DEBIT card requires primaryAccountId"));
                    if ("CREDIT".equals(entity.getCardType()) && entity.getCreditId() == null)
                        return Mono.error(new IllegalStateException("CREDIT card requires creditId"));

                    entity.setAccounts(normalizeAccounts(entity.getPrimaryAccountId(), entity.getAccounts()));
                    if (entity.getCreationDate() == null) entity.setCreationDate(OffsetDateTime.now());
                    entity.setUpdatedDate(OffsetDateTime.now());

                    return repo.save(entity).map(mapper::toResponse);
                });
    }

    @Override
    public Flux<CardResponse> listCards() {
        return repo.findAll().map(mapper::toResponse);
    }

    @Override
    public Mono<CardResponse> getCardById(String id) {
        return repo.findById(id).map(mapper::toResponse);
    }

    @Override
    public Mono<CardResponse> updateCard(String id, CardRequest request) {
        return repo.findById(id)
                .flatMap(db -> {
                    Card upd = mapper.toEntity(request);
                    upd.setId(db.getId());
                    upd.setCreationDate(db.getCreationDate());
                    upd.setUpdatedDate(OffsetDateTime.now());
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
            if (!"DEBIT".equals(c.getCardType())) return Mono.error(new IllegalStateException("Not a DEBIT card"));
            if (body.getAccounts() == null || body.getAccounts().isEmpty())
                return Mono.error(new IllegalStateException("accounts required"));
            if (!body.getAccounts().contains(body.getPrimaryAccountId()))
                return Mono.error(new IllegalStateException("primaryAccountId must be included"));
            c.setPrimaryAccountId(body.getPrimaryAccountId());
            c.setAccounts(normalizeAccounts(body.getPrimaryAccountId(), body.getAccounts()));
            c.setUpdatedDate(OffsetDateTime.now());
            return repo.save(c).map(mapper::toResponse);
        });
    }

    @Override
    public Mono<CardResponse> addAccount(String id, AddAccountRequest body) {
        return repo.findById(id).flatMap(c -> {
            if (!"DEBIT".equals(c.getCardType())) return Mono.error(new IllegalStateException("Not a DEBIT card"));
            if (!c.getAccounts().contains(body.getAccountId())) c.getAccounts().add(body.getAccountId());
            c.setAccounts(normalizeAccounts(c.getPrimaryAccountId(), c.getAccounts()));
            c.setUpdatedDate(OffsetDateTime.now());
            return repo.save(c).map(mapper::toResponse);
        });
    }

    @Override
    public Mono<CardResponse> removeAccount(String id, String accountId) {
        return repo.findById(id).flatMap(c -> {
            if (!"DEBIT".equals(c.getCardType())) return Mono.error(new IllegalStateException("Not a DEBIT card"));
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
            if (!"DEBIT".equals(c.getCardType())) return Mono.error(new IllegalStateException("Not a DEBIT card"));
            if (!body.getAccountIds().contains(c.getPrimaryAccountId()))
                return Mono.error(new IllegalStateException("Order must include primaryAccount"));
            c.setAccounts(normalizeAccounts(c.getPrimaryAccountId(), body.getAccountIds()));
            c.setUpdatedDate(OffsetDateTime.now());
            return repo.save(c).map(mapper::toResponse);
        });
    }

    @Override
    public Mono<CardResponse> setPrimaryAccount(String id, SetPrimaryAccountRequest body) {
        return repo.findById(id).flatMap(c -> {
            if (!"DEBIT".equals(c.getCardType())) return Mono.error(new IllegalStateException("Not a DEBIT card"));
            if (!c.getAccounts().contains(body.getAccountId()))
                return Mono.error(new IllegalStateException("Account not associated"));
            c.setPrimaryAccountId(body.getAccountId());
            c.setAccounts(normalizeAccounts(body.getAccountId(), c.getAccounts()));
            c.setUpdatedDate(OffsetDateTime.now());
            return repo.save(c).map(mapper::toResponse);
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