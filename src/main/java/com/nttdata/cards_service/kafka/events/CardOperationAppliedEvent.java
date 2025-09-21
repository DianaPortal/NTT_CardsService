package com.nttdata.cards_service.kafka.events;

import lombok.*;

import java.math.*;
import java.time.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardOperationAppliedEvent {
  private String operationId;
  private String cardId;
  private String type;         // "debit" | "credit"
  private BigDecimal amount;
  private String traceId;
  private Instant processedAt;
}
