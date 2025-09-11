package com.nttdata.cards_service.service;

import com.nttdata.cards_service.integration.accounts.*;
import com.nttdata.cards_service.integration.accounts.dto.*;
import com.nttdata.cards_service.integration.transactions.*;
import com.nttdata.cards_service.integration.transactions.dto.*;
import com.nttdata.cards_service.model.*;
import com.nttdata.cards_service.model.entity.*;
import com.nttdata.cards_service.model.value.*;
import com.nttdata.cards_service.repository.*;
import lombok.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import static com.nttdata.cards_service.service.CardDomainUtils.*;
import static com.nttdata.cards_service.service.CardOps.*;

@Service
@RequiredArgsConstructor
public class DebitOrchestratorService {

  private static final int KEEP_LAST_OPS = 200;

  private final CardRepository cardRepo;
  private final AccountsClient accounts;
  private final TransactionsClient tx;


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
    if (!"ACTIVE".equals(card.getStatus())) return Mono.error(new IllegalStateException("Card is not ACTIVE"));

    // Validación de límites (simple; puedes extenderla con límites por transacción/día)
    if ("withdrawal".equalsIgnoreCase(txType) && card.getLimits() != null && card.getLimits().getAtmWithdrawalLimit() != null) {
      Double max = card.getLimits().getAtmWithdrawalLimit();
      if (max != null && amount > max) {
        return Mono.error(new IllegalStateException("ATM withdrawal limit exceeded"));
      }
    }

    List<String> ordered = normalizeAccounts(card.getPrimaryAccountId(), card.getAccounts());

    return Flux.fromIterable(ordered)
        .concatMap(accId ->
            accounts.getAccount(accId)
                .map(a -> new AbstractMap.SimpleEntry<>(accId, a.getBalance()))
        )
        // Mapa preservando orden de asociación: accountId -> balance
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
                        // transacción por slice
                        TxPost post = new TxPost();
                        post.setType(txType); // withdrawal / purchase / payment
                        post.setAmount(s.getAmount());
                        post.setSender(TxProduct.of(s.getAccountId(), "savings_account"));
                        return tx.create(post);
                      })
                      .then(Mono.defer(() -> {
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
                      }))
              );
        });
  }
  // Compensación: deposita a cada cuenta lo debitado y registra una transacción de reversa
  public Mono<Void> compensate(Card card, String operationId, List<CardOperationResponseSlices> slices, String failedTxType) {
    return Flux.fromIterable(slices)
        .index()
        .concatMap(t -> {
          long idx = t.getT1();
          CardOperationResponseSlices s = t.getT2();

          BalanceOperationRequest req = new BalanceOperationRequest();
          req.setOperationId(operationId + "#comp#" + idx);
          req.setType("deposit");
          req.setAmount(s.getAmount());
          req.setMetadata(Map.of("compensationOf", operationId, "reason", "credits_failed"));

          return accounts.applyBalanceOperation(s.getAccountId(), req)
              .then(Mono.defer(() -> {
                TxPost rev = new TxPost();
                rev.setType("reversal");
                rev.setAmount(s.getAmount());
                rev.setReceiver(TxProduct.of(s.getAccountId(), "savings_account"));
                return tx.create(rev);
              }));
        })
        .then();
  }


}