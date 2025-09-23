package com.nttdata.cards_service.kafka.events;

import lombok.*;

import java.time.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardOperationDeniedEvent {
  private String operationId;
  private String cardId;
  private String type;         // "debit" | "credit"
  private String reason;
  private String traceId;
  private Instant processedAt;
}
