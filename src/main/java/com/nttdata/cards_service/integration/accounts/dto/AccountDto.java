package com.nttdata.cards_service.integration.accounts.dto;

public class AccountDto {
    private String id;
    private String accountType;
    private Double balance;

    public String getId() {
        return id;
    }

    public String getAccountType() {
        return accountType;
    }

    public Double getBalance() {
        return balance;
    }
}
