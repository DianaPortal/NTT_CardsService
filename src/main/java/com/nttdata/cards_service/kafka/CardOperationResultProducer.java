package com.nttdata.cards_service.kafka;

import com.nttdata.cards_service.kafka.events.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.*;
import org.springframework.stereotype.*;

import java.math.*;
import java.time.*;

@Service
@RequiredArgsConstructor
public class CardOperationResultProducer {

  private final KafkaTemplate<String, Object> kafka;

  @Value("${app.topics.card-debit-applied}")
  private String topicDebitOk;
  @Value("${app.topics.card-debit-denied}")
  private String topicDebitKo;
  @Value("${app.topics.card-credit-applied}")
  private String topicCreditOk;
  @Value("${app.topics.card-credit-denied}")
  private String topicCreditKo;

  public void publishDebitApplied(String opId, String cardId, BigDecimal amount, String traceId) {
    var evt = new CardOperationAppliedEvent(opId, cardId, "debit", amount, traceId, Instant.now());
    kafka.send(topicDebitOk, cardId, evt);
  }

  public void publishDebitDenied(String opId, String cardId, String reason, String traceId) {
    var evt = new CardOperationDeniedEvent(opId, cardId, "debit", reason, traceId, Instant.now());
    kafka.send(topicDebitKo, cardId, evt);
  }

  public void publishCreditApplied(String opId, String cardId, BigDecimal amount, String traceId) {
    var evt = new CardOperationAppliedEvent(opId, cardId, "credit", amount, traceId, Instant.now());
    kafka.send(topicCreditOk, cardId, evt);
  }

  public void publishCreditDenied(String opId, String cardId, String reason, String traceId) {
    var evt = new CardOperationDeniedEvent(opId, cardId, "credit", reason, traceId, Instant.now());
    kafka.send(topicCreditKo, cardId, evt);
  }
}
