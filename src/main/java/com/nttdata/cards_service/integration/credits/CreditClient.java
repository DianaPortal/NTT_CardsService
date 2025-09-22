package com.nttdata.cards_service.integration.credits;

import com.nttdata.cards_service.integration.credits.dto.*;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.reactor.circuitbreaker.operator.*;
import io.github.resilience4j.reactor.timelimiter.*;
import io.github.resilience4j.timelimiter.*;
import lombok.extern.slf4j.*;
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

  public CreditClient(WebClient creditsWebClient,
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
        .onStatus(sc -> sc.value() == 404,
            resp -> Mono.error(new CustomerNotFoundException(customerId)))
        .bodyToMono(OverdueRes.class)
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("credits")))
        .onErrorMap(TimeoutException.class,
            ex -> new ResponseStatusException(GATEWAY_TIMEOUT, "Timeout Credits (2s)", ex))
        .onErrorResume(CustomerNotFoundException.class, ex ->
            Mono.just(new OverdueRes(false)));
  }

  public Mono<Void> applyPayment(String creditId, CreditPaymentRequest req) {
    CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("credits");
    log.debug("POST Credits /credits/{}/payments amount={}", creditId, req.getAmount());
    return web.post()
        .uri("/credits/{id}/payments", creditId)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve()
        .bodyToMono(Void.class)
        .transformDeferred(CircuitBreakerOperator.of(cb))
        .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("credits")))
        .onErrorMap(TimeoutException.class,
            ex -> new ResponseStatusException(GATEWAY_TIMEOUT, "Timeout Credits (2s)", ex));
  }

  public static class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String id) {
      super("Customer not found: " + id);
    }
  }
}
