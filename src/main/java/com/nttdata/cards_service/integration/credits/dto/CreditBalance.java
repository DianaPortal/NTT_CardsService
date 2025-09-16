package com.nttdata.cards_service.integration.credits.dto;

import lombok.*;
@Data
public class CreditBalance {
    private Double limit;
    private Double balance;
    private Double available;


}