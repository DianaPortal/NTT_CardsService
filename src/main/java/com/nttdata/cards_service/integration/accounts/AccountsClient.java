package com.nttdata.cards_service.integration.accounts;

import com.nttdata.cards_service.integration.accounts.dto.AccountDto;
import com.nttdata.cards_service.integration.accounts.dto.BalanceOperationRequest;
import com.nttdata.cards_service.integration.accounts.dto.BalanceOperationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AccountsClient {
    private final WebClient web;

    public AccountsClient(WebClient accountsWebClient) {
        this.web = accountsWebClient;
    }

    //Obtener una cuenta por id
    public Mono<AccountDto> getAccount(String id) {
        log.debug("GET Accounts /api/accounts/{}", id);
        return web.get().uri("/api/accounts/{id}", id)
                .retrieve().bodyToMono(AccountDto.class);
    }

    //Aplica - Operación de saldo: débito o crédito  a una cuenta
    public Mono<BalanceOperationResponse> applyBalanceOperation(String accountId, BalanceOperationRequest req) {
        log.debug("POST Accounts /api/accounts/{}/balance-ops body={}", accountId, req);
        return web.post().uri("/api/accounts/{id}/balance-ops", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve().bodyToMono(BalanceOperationResponse.class);
    }
}