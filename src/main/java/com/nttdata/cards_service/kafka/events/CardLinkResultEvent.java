package com.nttdata.cards_service.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor
public class CardLinkResultEvent {
    private String phone;
    private String debitCardId;
    private String status;      // "OK" | "REJECTED"
    private String reason;
    private String traceId;
    private Instant processedAt;
}