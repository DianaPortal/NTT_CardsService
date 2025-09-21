package com.nttdata.cards_service.kafka.events;

import lombok.*;

import java.time.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardLinkResultEvent {
  private String phone;
  private String debitCardId;
  private String status;      // "OK" | "REJECTED"
  private String reason;
  private String traceId;
  private Instant processedAt;
}
