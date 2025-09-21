package com.nttdata.cards_service.integration.credits;

import com.nttdata.cards_service.integration.credits.dto.*;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.reactor.circuitbreaker.operator.*;
import io.github.resilience4j.reactor.timelimiter.*;
import io.github.resilience4j.timelimiter.*;
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
public class CreditClient {
  private final WebClient web;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final TimeLimiterRegistry timeLimiterRegistry;

  public CreditClient(@Qualifier("creditsWebClient") WebClient creditsWebClient,
                      CircuitBreakerRegistry circuitBreakerRegistry,
                      TimeLimiterRegistry timeLimiterRegistry) {
    this.web = creditsWebClient;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.timeLimiterRegistry = timeLimiterRegistry;
  }

  // Estado de deuda del cliente

  public Mono<OverdueRes> hasOverdue(String customerId) {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("credits");
    log.debug("GET Credits /credits/{}/debt-status", customerId);
    return web.get()
        .uri("/credits/{customerId}/debt-status", customerId)
        .retrieve()
        .onStatus(org.springframework.http.HttpStatus::is4xxClientError, resp ->
            resp.statusCode() == org.springframework.http.HttpStatus.NOT_FOUND
                ? Mono.error(new CustomerNotFoundException(customerId))
                : resp.createException()
        )
        .bodyToMono(OverdueRes.class)
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("credits")))
        .onErrorMap(TimeoutException.class,
            ex -> new ResponseStatusException(GATEWAY_TIMEOUT, "Timeout en Credits (2s)", ex))
        // Decide política: si cliente no existe lo tratamos como "sin deuda"
        .onErrorResume(CustomerNotFoundException.class, ex -> {
          log.info("Customer {} no existe en créditos, asumimos sin deuda", customerId);
          return Mono.just(new OverdueRes(false));
        });

  }

  // Registrar un pago a un crédito (ignoramos el body 201)
  public Mono<Void> applyPayment(String creditId, CreditPaymentRequest req) {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("credits");
    log.debug("POST Credits /credits/{}/payments body={}", creditId, req);
    return web.post().uri("/credits/{id}/payments", creditId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve()
        .toBodilessEntity()
        .then()
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("credits")))
        .onErrorMap(TimeoutException.class,
            ex -> new ResponseStatusException(GATEWAY_TIMEOUT, "Timeout en Credits (2s)", ex));
  }


  public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String customerId) {
      super("Customer not found: " + customerId);
    }
  }
}
