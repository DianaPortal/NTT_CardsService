package com.nttdata.cards_service.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardLinkRequestEvent {
    private String phone;
    private String documentType;
    private String documentNumber;
    private String debitCardId;
    private String traceId;
}
