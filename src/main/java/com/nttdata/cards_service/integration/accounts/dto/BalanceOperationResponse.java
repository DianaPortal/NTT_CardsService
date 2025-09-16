package com.nttdata.cards_service.integration.accounts.dto;

import lombok.*;

@Data
public class BalanceOperationResponse {
  private Boolean applied;
  private Double newBalance;
  private Double commissionApplied;
  private String message;


}