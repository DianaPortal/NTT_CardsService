package com.nttdata.cards_service.integration.credits.dto;

import lombok.*;

@Data
public class OverdueRes {
  private boolean hasOverdue;
  private Double totalOverdueAmount;

  public OverdueRes() {

  }
  public OverdueRes(boolean hasOverdue) {
    this.hasOverdue = hasOverdue;
  }

}
