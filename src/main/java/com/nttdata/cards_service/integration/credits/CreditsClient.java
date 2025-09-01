package com.nttdata.cards_service.integration.credits;

import com.nttdata.cards_service.integration.credits.dto.CreditBalance;
import com.nttdata.cards_service.integration.credits.dto.CreditPaymentRequest;
import com.nttdata.cards_service.integration.credits.dto.OverdueRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CreditsClient {
    private final WebClient web;

    public CreditsClient(WebClient creditsWebClient) {
        this.web = creditsWebClient;
    }

    //Consultar si un cliente tiene deudas vencidas
    public Mono<OverdueRes> hasOverdue(String customerId) {
        log.debug("GET Credits /credits/overdue/{}", customerId);
        return web.get().uri("/credits/overdue/{cid}", customerId)

                .retrieve().bodyToMono(OverdueRes.class);
    }

    //Registrar un pago a un crédito
    public Mono<CreditBalance> applyPayment(String creditId, CreditPaymentRequest req) {
        log.debug("POST Credits /credits/{}/payments body={}", creditId, req);
        return web.post().uri("/credits/{id}/payments", creditId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req).retrieve().bodyToMono(CreditBalance.class);
    }
}
