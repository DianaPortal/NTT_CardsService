package com.nttdata.cards_service.integration.credits.dto;
import lombok.*;
@Data
public class CreditPaymentRequest {
    private Double amount;
    private String note;
    // NUEVO: quién paga (dueño de la tarjeta de débito)
    private String payerCustomerId;

    // NUEVO: canal del pago (conforme al contrato de Credits)
    // enum: CASH, TRANSFER, CARD
    private String channel;


}
