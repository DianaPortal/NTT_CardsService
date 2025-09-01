package com.nttdata.cards_service.integration.transactions;

import com.nttdata.cards_service.integration.transactions.dto.TxGet;
import com.nttdata.cards_service.integration.transactions.dto.TxPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class TransactionsClient {
    private final WebClient web;

    public TransactionsClient(WebClient transactionsWebClient) {
        this.web = transactionsWebClient;
    }

    //Crear una Transacción
    public Mono<TxGet> create(TxPost req) {
        log.debug("POST Transactions /transactions body={}", req);
        return web.post().uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req).retrieve().bodyToMono(TxGet.class);
    }

    // Consulta transacciones por producto (cardId)
    public Flux<TxGet> findByProduct(String productId) {
        log.debug("GET Transactions /transactions/product/{}", productId);
        return web.get().uri("/transactions/product/{id}", productId)
                .retrieve().bodyToFlux(TxGet.class);
    }
}
