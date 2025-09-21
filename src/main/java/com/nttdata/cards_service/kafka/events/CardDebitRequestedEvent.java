package com.nttdata.cards_service.kafka.events;

import lombok.*;

import java.math.*;
import java.time.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDebitRequestedEvent {
  private String operationId;  // p.ej. paymentId+":card:debit"
  private String cardId;       // tarjeta del EMISOR
  private BigDecimal amount;
  private String traceId;
  private Instant requestedAt;
}
