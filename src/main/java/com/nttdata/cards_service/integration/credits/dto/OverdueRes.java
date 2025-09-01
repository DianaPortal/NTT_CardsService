package com.nttdata.cards_service.integration.credits.dto;

public class OverdueRes {
    private boolean hasOverdue;
    private Double totalOverdueAmount;

    public boolean isHasOverdue() {
        return hasOverdue;
    }   // boolean getter estándar

    public boolean getHasOverdue() {
        return hasOverdue;
    }  // por si el llamado usa get*

    public Double getTotalOverdueAmount() {
        return totalOverdueAmount;
    }
}
