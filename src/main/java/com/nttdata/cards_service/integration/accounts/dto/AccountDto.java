package com.nttdata.cards_service.integration.accounts.dto;
import lombok.*;

@Data
public class AccountDto {

    private String id;
    private String accountType;
    private Double balance;

    private Boolean active;


}
