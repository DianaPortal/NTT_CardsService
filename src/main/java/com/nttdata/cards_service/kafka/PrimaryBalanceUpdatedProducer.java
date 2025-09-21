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
public class PrimaryBalanceUpdatedProducer {

  private final KafkaTemplate<String, Object> kafka;

  @Value("${app.topics.card-primary-balance-updated}")
  private String topic;

  public void publish(String cardId, String accountId, BigDecimal balance, String traceId) {
    kafka.send(topic, cardId, new PrimaryBalanceUpdatedEvent(cardId, accountId, balance, traceId, Instant.now()));
  }
}
