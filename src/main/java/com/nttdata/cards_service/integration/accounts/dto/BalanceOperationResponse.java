package com.nttdata.cards_service.integration.accounts.dto;

public class BalanceOperationResponse {
    private Boolean applied;
    private Double newBalance;
    private Double commissionApplied;
    private String message;

    public Boolean getApplied() {
        return applied;
    }

    public Double getNewBalance() {
        return newBalance;
    }

    public Double getCommissionApplied() {
        return commissionApplied;
    }

    public String getMessage() {
        return message;
    }
}