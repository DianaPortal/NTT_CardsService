package com.nttdata.cards_service.integration.transactions.dto;

import java.time.OffsetDateTime;

public class TxGet {
    private String id;
    private Integer number;
    private TxProduct sender;
    private TxProduct receiver;
    private String type;
    private Double amount;
    private OffsetDateTime createdDate;

    public String getId() {
        return id;
    }

    public Integer getNumber() {
        return number;
    }

    public TxProduct getSender() {
        return sender;
    }

    public TxProduct getReceiver() {
        return receiver;
    }

    public String getType() {
        return type;
    }

    public Double getAmount() {
        return amount;
    }

    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }
}

