package com.nttdata.cards_service.service;

import com.nttdata.cards_service.integration.accounts.AccountsClient;
import com.nttdata.cards_service.integration.accounts.dto.BalanceOperationRequest;
import com.nttdata.cards_service.integration.credits.CreditsClient;
import com.nttdata.cards_service.integration.credits.dto.CreditPaymentRequest;
import com.nttdata.cards_service.integration.transactions.TransactionsClient;
import com.nttdata.cards_service.integration.transactions.dto.TxPost;
import com.nttdata.cards_service.integration.transactions.dto.TxProduct;
import com.nttdata.cards_service.model.CardOperationResponse;
import com.nttdata.cards_service.model.CardOperationResponseSlices;
import com.nttdata.cards_service.model.entity.Card;
import com.nttdata.cards_service.model.value.Slice;
import com.nttdata.cards_service.model.value.StoredOperation;
import com.nttdata.cards_service.repository.CardRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.nttdata.cards_service.service.CardOps.findOperation;
import static com.nttdata.cards_service.service.CardOps.upsertOperation;
import static com.nttdata.cards_service.service.CardDomainUtils.normalizeAccounts;
import static com.nttdata.cards_service.service.CardDomainUtils.planSlices;

@Service
public class PayCreditService {
    private final DebitOrchestratorService debit;
    private final CreditsClient credits;
    private final TransactionsClient tx;

    public PayCreditService(DebitOrchestratorService debit, CreditsClient credits, TransactionsClient tx) {
        this.debit = debit;
        this.credits = credits;
        this.tx = tx;
    }

    public Mono<StoredOperation> pay(String cardId, String opId, String creditId, double amount, String note) {
        Map<String, Object> md = Map.of("purpose", "pay-credit", "creditId", creditId);
        return debit.debit(cardId, opId, amount, "PAY_CREDIT", md, "payment")
                .flatMap(stored ->
                        credits.applyPayment(creditId, new CreditPaymentRequest(amount, note))
                                .flatMap(cb -> {
                                    TxPost post = new TxPost();
                                    post.setType("payment");
                                    post.setAmount(amount);
                                    post.setReceiver(TxProduct.of(creditId, "personal_credit"));
                                    return tx.create(post).thenReturn(stored);
                                })
                );
    }

    @Service
    public static class DebitOrchestratorService {

        private static final int KEEP_LAST_OPS = 200;

        private final CardRepository cardRepo;
        private final AccountsClient accounts;
        private final TransactionsClient tx;

        public DebitOrchestratorService(CardRepository cardRepo, AccountsClient accounts, TransactionsClient tx) {
            this.cardRepo = cardRepo;
            this.accounts = accounts;
            this.tx = tx;
        }

        public Mono<StoredOperation> debit(String cardId, String operationId, double amount,
                                           String opKind, Map<String, Object> metadata, String txType) {

            return cardRepo.findById(cardId)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("Card not found")))
                    .flatMap(card -> findOperation(card, operationId).map(Mono::just)
                            .orElseGet(() -> process(card, operationId, amount, opKind, metadata, txType)));
        }

        private Mono<StoredOperation> process(Card card, String operationId, double amount,
                                              String opKind, Map<String, Object> metadata, String txType) {

            if (!"DEBIT".equals(card.getCardType())) return Mono.error(new IllegalStateException("Not a DEBIT card"));
            if (!"ACTIVE".equals(card.getStatus()))  return Mono.error(new IllegalStateException("Card is not ACTIVE"));

            List<String> ordered = normalizeAccounts(card.getPrimaryAccountId(), card.getAccounts());

            return Flux.fromIterable(ordered)
                    .concatMap(accId ->
                            accounts.getAccount(accId)
                                    .map(a -> new AbstractMap.SimpleEntry<>(accId, a.getBalance()))
                    )
                    // TIPADO explícito del collect (contiene <String, Double>):
                    .collectMap(Map.Entry::getKey, Map.Entry::getValue, java.util.LinkedHashMap::new)
                    .flatMap((var balances) -> {
                        List<Slice> plan = planSlices(amount, (LinkedHashMap<String, Double>) balances);

                        return Flux.fromIterable(plan)
                                .index()
                                .concatMap(t -> {
                                    long idx = t.getT1();
                                    Slice s = t.getT2();

                                    BalanceOperationRequest req = new BalanceOperationRequest();
                                    req.setOperationId(operationId + "#" + idx);
                                    req.setType("withdrawal");
                                    req.setAmount(s.getAmount());
                                    req.setMetadata(metadata);

                                    return accounts.applyBalanceOperation(s.getAccountId(), req)
                                            .map(resp -> {
                                                CardOperationResponseSlices x = new CardOperationResponseSlices();
                                                x.setAccountId(s.getAccountId());
                                                x.setAmount(s.getAmount());
                                                x.setCommissionApplied(resp.getCommissionApplied() == null ? 0.0 : resp.getCommissionApplied());
                                                return x;
                                            });
                                })
                                .collectList()
                                .flatMap((List<CardOperationResponseSlices> slices) ->
                                        Flux.fromIterable(slices)
                                                .concatMap(s -> {
                                                    TxPost post = new TxPost();
                                                    post.setType(txType);
                                                    post.setAmount(s.getAmount());
                                                    post.setSender(TxProduct.of(s.getAccountId(), "savings_account"));
                                                    return tx.create(post);
                                                })
                                                .then(Mono.just(slices))
                                )
                                .flatMap((List<CardOperationResponseSlices> slices) -> {
                                    double commission = slices.stream()
                                            .collect(Collectors.summingDouble(CardOperationResponseSlices::getCommissionApplied));

                                    CardOperationResponse result = new CardOperationResponse();
                                    result.setApplied(true);
                                    result.setTotalAmount(amount);
                                    result.setCommissionTotal(commission);
                                    result.setSlices(slices);
                                    result.setMessage("OK");

                                    StoredOperation stored = new StoredOperation();
                                    stored.setId(operationId);
                                    stored.setKind(opKind);
                                    stored.setCreatedAt(OffsetDateTime.now());
                                    stored.setResult(result);

                                    upsertOperation(card, stored, KEEP_LAST_OPS);
                                    card.setUpdatedDate(OffsetDateTime.now());
                                    return cardRepo.save(card).thenReturn(stored);
                                });
                    });
        }

    }
}
