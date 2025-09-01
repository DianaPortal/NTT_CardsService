package com.nttdata.cards_service.integration.credits.dto;


public class CreditPaymentRequest {
    private Double amount;
    private String note;

    public CreditPaymentRequest() {
    }

    public CreditPaymentRequest(Double amount, String note) {
        this.amount = amount;
        this.note = note;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
