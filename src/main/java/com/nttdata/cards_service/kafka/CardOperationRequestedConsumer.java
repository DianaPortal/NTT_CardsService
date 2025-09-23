package com.nttdata.cards_service.kafka;

import com.nttdata.cards_service.integration.accounts.*;
import com.nttdata.cards_service.integration.accounts.dto.*;
import com.nttdata.cards_service.kafka.events.*;
import com.nttdata.cards_service.model.entity.*;
import com.nttdata.cards_service.model.value.*;
import com.nttdata.cards_service.repository.*;
import com.nttdata.cards_service.service.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.kafka.annotation.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

import java.math.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class CardOperationRequestedConsumer {

  private static final String DEBIT = "DEBIT";

  private final CardRepository repo;
  private final DebitOrchestratorService debitOrchestrator; // ya existe
  private final AccountsClient accountsClient;              // REST interno a Accounts
  private final CardOperationResultProducer results;
  private final PrimaryBalanceUpdatedProducer balanceEvents;

  // --- DÉBITO (Emisor con tarjeta DEBIT) ---
  @KafkaListener(topics = "${app.topics.card-debit-req}", groupId = "${spring.kafka.consumer.group-id}")
  public void onCardDebitRequested(CardDebitRequestedEvent evt) {
    log.info("[CARDS] debit.requested opId={} cardId={} amount={}", evt.getOperationId(), evt.getCardId(), evt.getAmount());

    repo.findById(evt.getCardId())
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Card not found")))
        .flatMap(card -> {
          if (!DEBIT.equalsIgnoreCase(card.getCardType()))
            return Mono.error(new IllegalStateException("Not a DEBIT card"));

          // orquesta el débito (usa fallback/preferencias que tengas)
          return debitOrchestrator.debit(
                  card.getId(),
                  evt.getOperationId(),
                  evt.getAmount().doubleValue(),
                  "YANKI_P2P_DEBIT",
                  java.util.Map.of("source", "yanki", "use", "p2p", "noRefund", "true"),
                  "purchase"
              ).map(StoredOperation::getResult)
              .flatMap(res -> {
                if (Boolean.TRUE.equals(res.getApplied())) {
                  results.publishDebitApplied(evt.getOperationId(), evt.getCardId(), evt.getAmount(), evt.getTraceId());
                  // Emitir saldo actualizado (leer balance de la cuenta principal)
                  return publishPrimaryBalance(card, evt.getTraceId()).thenReturn(true);
                } else {
                  results.publishDebitDenied(evt.getOperationId(), evt.getCardId(), "Denied by rules", evt.getTraceId());
                  return Mono.just(false);
                }
              });
        })
        .doOnError(e -> results.publishDebitDenied(evt.getOperationId(), evt.getCardId(), e.getMessage(), evt.getTraceId()))
        .subscribe();
  }

  // --- CRÉDITO (Receptor con tarjeta DEBIT) ---
  @KafkaListener(topics = "${app.topics.card-credit-req}", groupId = "${spring.kafka.consumer.group-id}")
  public void onCardCreditRequested(CardDebitTransferInRequestedEvent evt) {
    log.info("[CARDS] credit.requested opId={} cardId={} amount={}", evt.getOperationId(), evt.getCardId(), evt.getAmount());

    repo.findById(evt.getCardId())
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Card not found")))
        .flatMap(card -> {
          if (!DEBIT.equalsIgnoreCase(card.getCardType()))
            return Mono.error(new IllegalStateException("Not a DEBIT card"));
          if (card.getPrimaryAccountId() == null || card.getPrimaryAccountId().isBlank())
            return Mono.error(new IllegalStateException("No primary account"));

          // Abono a la cuenta principal (transfer_in)
          BalanceOperationRequest req = new BalanceOperationRequest();
          req.setOperationId(evt.getOperationId());
          req.setType("transfer_in");
          req.setAmount(evt.getAmount().doubleValue());
          req.setMetadata(java.util.Map.of("source", "yanki", "use", "p2p"));

          return accountsClient.applyBalanceOperation(card.getPrimaryAccountId(), req)
              .map(res -> res)  // contiene newBalance
              .doOnSuccess(res -> {
                results.publishCreditApplied(evt.getOperationId(), evt.getCardId(), evt.getAmount(), evt.getTraceId());
                balanceEvents.publish(evt.getCardId(), card.getPrimaryAccountId(), BigDecimal.valueOf(res.getNewBalance()), evt.getTraceId());
              });
        })
        .doOnError(e -> results.publishCreditDenied(evt.getOperationId(), evt.getCardId(), e.getMessage(), evt.getTraceId()))
        .subscribe();
  }

  private Mono<Void> publishPrimaryBalance(Card card, String traceId) {
    if (card.getPrimaryAccountId() == null || card.getPrimaryAccountId().isBlank())
      return Mono.empty();
    // leer saldo actual para el evento (GET account)
    return accountsClient.getAccount(card.getPrimaryAccountId())
        .doOnNext(acc -> balanceEvents.publish(card.getId(), acc.getId(), BigDecimal.valueOf(acc.getBalance()), traceId))
        .then();
  }
}