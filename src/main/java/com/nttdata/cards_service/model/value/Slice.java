package com.nttdata.cards_service.model.value;


public class Slice {
    private final String accountId;
    private final double amount;

    public Slice(String accountId, double amount) {
        this.accountId = accountId;
        this.amount = amount;
    }

    public String getAccountId() {
        return accountId;
    }

    public double getAmount() {
        return amount;
    }
}