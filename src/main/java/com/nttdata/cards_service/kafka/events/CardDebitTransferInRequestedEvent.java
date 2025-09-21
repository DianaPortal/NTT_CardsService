package com.nttdata.cards_service.kafka.events;

import lombok.*;

import java.math.*;
import java.time.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDebitTransferInRequestedEvent {
  private String operationId;
  private String cardId;       // tarjeta del RECEPTOR
  private BigDecimal amount;
  private String traceId;
  private Instant requestedAt;

}
