package com.nttdata.cards_service.integration.transactions;

import com.nttdata.cards_service.integration.transactions.dto.TxGet;
import com.nttdata.cards_service.integration.transactions.dto.TxPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
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
public class TransactionsClient {
    private final WebClient web;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    public TransactionsClient(@Qualifier("transactionsWebClient") WebClient webClient,
                          CircuitBreakerRegistry circuitBreakerRegistry,
                          TimeLimiterRegistry timeLimiterRegistry) {
        this.web= webClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    //Crear una Transacción
    public Mono<TxGet> create(TxPost req) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("transactions");
        log.debug("POST Transactions /transactions body={}", req);
        return web.post().uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req).retrieve().bodyToMono(TxGet.class)
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("transactions")))
                .onErrorMap(TimeoutException.class, ex -> new ResponseStatusException(GATEWAY_TIMEOUT, "Timeout en Transactions (2s)", ex));
    }

    // Consulta transacciones por producto (cardId)
    public Flux<TxGet> findByProduct(String productId) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("transactions");
        log.debug("GET Transactions /transactions/product/{}", productId);
        return web.get().uri("/transactions/product/{id}", productId)
                .retrieve().bodyToFlux(TxGet.class)
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .transformDeferred(TimeLimiterOperator.of(timeLimiterRegistry.timeLimiter("transactions")))
                .onErrorMap(TimeoutException.class, ex -> new ResponseStatusException(GATEWAY_TIMEOUT,"Timeout en Transactions (2s)", ex));
    }
}
