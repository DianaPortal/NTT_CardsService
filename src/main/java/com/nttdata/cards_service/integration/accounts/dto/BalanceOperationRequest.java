package com.nttdata.cards_service.integration.accounts.dto;

import java.util.Map;

public class BalanceOperationRequest {
    private String operationId;
    private String type;       // deposit | withdrawal | transfer_in | transfer_out | commission
    private Double amount;
    private Map<String, Object> metadata;

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
