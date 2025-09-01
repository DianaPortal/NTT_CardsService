package com.nttdata.cards_service.integration.credits.dto;

public class CreditBalance {
    private Double limit;
    private Double balance;
    private Double available;

    public Double getLimit() {
        return limit;
    }

    public Double getBalance() {
        return balance;
    }

    public Double getAvailable() {
        return available;
    }
}