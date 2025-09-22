package com.nttdata.cards_service.integration.accounts;

import com.nttdata.cards_service.integration.accounts.dto.*;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.reactor.circuitbreaker.operator.*;
import io.github.resilience4j.reactor.timelimiter.*;
import io.github.resilience4j.timelimiter.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;

import java.util.concurrent.*;

import static org.springframework.http.HttpStatus.*;

@Component
@Slf4j
public class AccountsClient {


  private final WebClient webClient;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final TimeLimiterRegistry timeLimiterRegistry;

  public AccountsClient(@Qualifier("accountsWebClient") WebClient webClient,
                        CircuitBreakerRegistry circuitBreakerRegistry,
                        TimeLimiterRegistry timeLimiterRegistry) {
    this.webClient = webClient;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.timeLimiterRegistry = timeLimiterRegistry;
  }

  // Obtener una cuenta por id
  public Mono<AccountDto> getAccount(String id) {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("accounts");
    log.debug("GET Accounts /accounts/{}", id); // base-url incluye /api (según properties)
    return webClient.get()
        .uri("/accounts/{id}", id)
        .retrieve()
        .onStatus(s -> s.value()==404, r -> Mono.error(new IllegalArgumentException("Account not found")))
        .bodyToMono(AccountDto.class)
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("accounts")))
        .onErrorMap(TimeoutException.class,
            ex -> new ResponseStatusException(GATEWAY_TIMEOUT, "Timeout Accounts (2s)", ex));
  }

  public Mono<BalanceOperationResponse> applyBalanceOperation(String accountId, BalanceOperationRequest req) {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("accounts");
    log.debug("POST Accounts /accounts/{}/balance-ops opId={}", accountId, req.getOperationId());
    return webClient.post()
        .uri("/accounts/{id}/balance-ops", accountId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve()
        .onStatus(s -> s.value()==404, r -> Mono.error(new IllegalArgumentException("Account not found")))
        .bodyToMono(BalanceOperationResponse.class)
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("accounts")))
        .onErrorMap(TimeoutException.class,
            ex -> new ResponseStatusException(GATEWAY_TIMEOUT, "Timeout Accounts (2s)", ex));
  }
}
