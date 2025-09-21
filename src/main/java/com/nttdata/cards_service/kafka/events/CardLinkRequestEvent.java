package com.nttdata.cards_service.kafka.events;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardLinkRequestEvent {
  private String phone;
  private String debitCardId;
  private String traceId;
}
