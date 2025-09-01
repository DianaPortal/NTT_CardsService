package com.nttdata.cards_service.integration.transactions.dto;

public class TxPost {
    private TxProduct sender;
    private TxProduct receiver;
    private String type;
    private Double amount;

    public TxProduct getSender() {
        return sender;
    }

    public void setSender(TxProduct sender) {
        this.sender = sender;
    }

    public TxProduct getReceiver() {
        return receiver;
    }

    public void setReceiver(TxProduct receiver) {
        this.receiver = receiver;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}