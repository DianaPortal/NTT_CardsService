package com.nttdata.cards_service.integration.credits;

import com.nttdata.cards_service.integration.credits.dto.CreditBalance;
import com.nttdata.cards_service.integration.credits.dto.CreditPaymentRequest;
import com.nttdata.cards_service.integration.credits.dto.OverdueRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

@Component
@Slf4j
public class CreditsClient {
    private final WebClient web;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    public CreditsClient(WebClient creditsWebClient,
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
        return web.get().uri("/credits/{customerId}/debt-status", customerId)
            .retrieve().bodyToMono(OverdueRes.class)
            .transformDeferred(CircuitBreakerOperator.of(cb))
            .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("credits")))
            .onErrorMap(TimeoutException.class,
                ex -> new ResponseStatusException(GATEWAY_TIMEOUT, "Timeout en Credits (2s)", ex));
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
}
