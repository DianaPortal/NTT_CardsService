package com.nttdata.cards_service.integration.transactions.dto;

import lombok.*;

import java.util.*;

@Data
public class TransactionGetClientBalance {
  private TxProduct product;
  private List<TxGet> transactions;


}