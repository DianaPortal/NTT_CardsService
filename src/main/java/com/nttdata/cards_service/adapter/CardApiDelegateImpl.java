package com.nttdata.cards_service.adapter;

import com.nttdata.cards_service.model.*;
import com.nttdata.cards_service.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.nttdata.cards_service.api.ApiApiDelegate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardApiDelegateImpl implements ApiApiDelegate {

    private final CardService service;

    @Override
    public Mono<ResponseEntity<CardResponse>> registerCard(
            Mono<CardRequest> cardRequest,
            ServerWebExchange exchange) {
        log.info("POST /api/cards - registro de tarjeta");
        return cardRequest.flatMap(service::createCard)
                .doOnSuccess(res -> log.info("Tarjeta creada id={}", res == null ? null : res.getId()))
                .map(body -> ResponseEntity.status(201).body(body));
    }

    @Override
    public Mono<ResponseEntity<Flux<CardResponse>>> listCards(ServerWebExchange exchange) {
        log.info("GET /api/cards - listado de tarjetas");
        return Mono.just(ResponseEntity.ok(service.listCards()));
    }

    @Override
    public Mono<ResponseEntity<CardResponse>> getCardById(String id, ServerWebExchange exchange) {
        log.info("GET /api/cards/{} - obtener tarjeta", id);
        return service.getCardById(id).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CardResponse>> updateCard(String id, Mono<CardRequest> cardRequest, ServerWebExchange exchange) {
        log.info("PUT /api/cards/{} - actualizar tarjeta", id);
        return cardRequest.flatMap(req -> service.updateCard(id, req))
                .doOnSuccess(res -> log.info("Tarjeta {} actualizada", id))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteCard(String id, ServerWebExchange exchange) {
        log.info("DELETE /api/cards/{} - eliminar tarjeta", id);
        return service.deleteCard(id).thenReturn(ResponseEntity.noContent().build());
    }

    // Associations
    @Override
    public Mono<ResponseEntity<CardResponse>> replaceCardAccounts(String id, Mono<CardAccountsReplaceRequest> body, ServerWebExchange exchange) {
        log.info("PUT /api/cards/{}/accounts - reemplazar cuentas asociadas", id);
        return body.flatMap(b -> service.replaceAccounts(id, b))
                .doOnSuccess(res -> log.info("Cuentas de tarjeta {} reemplazadas (primary={})", id, res == null ? null : res.getPrimaryAccountId()))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CardResponse>> addCardAccount(String id, Mono<AddAccountRequest> body, ServerWebExchange exchange) {
        log.info("POST /api/cards/{}/accounts - agregar cuenta", id);
        return body.flatMap(b -> service.addAccount(id, b))
                .doOnSuccess(res -> log.info("Cuenta agregada a tarjeta {}", id))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CardResponse>> removeCardAccount(String id, String accountId, ServerWebExchange exchange) {
        log.info("DELETE /api/cards/{}/accounts/{} - eliminar cuenta asociada", id, accountId);
        return service.removeAccount(id, accountId).map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CardResponse>> reorderCardAccounts(String id, Mono<AccountReorderRequest> body, ServerWebExchange exchange) {
        log.info("PUT /api/cards/{}/accounts/reorder - reordenar cuentas", id);
        return body.flatMap(b -> service.reorderAccounts(id, b))
                .doOnSuccess(res -> log.info("Cuentas reordenadas para tarjeta {}", id))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CardResponse>> setPrimaryAccount(String id, Mono<SetPrimaryAccountRequest> body, ServerWebExchange exchange) {
        log.info("PUT /api/cards/{}/primary-account - definir principal", id);
        return body.flatMap(b -> service.setPrimaryAccount(id, b))
                .doOnSuccess(res -> log.info("Cuenta principal actualizada para tarjeta {}", id))
                .map(ResponseEntity::ok);
    }

    // Operations
    @Override
    public Mono<ResponseEntity<CardOperationResponse>> debitPayment(String id, Mono<DebitPaymentRequest> req, ServerWebExchange exchange) {
        log.info("POST /api/cards/{}/debit/payments - pago con débito", id);
        return req.flatMap(r -> service.debitPayment(id, r))
                .doOnSuccess(res -> log.info("Pago con débito aplicado? {} - total={}", res != null && Boolean.TRUE.equals(res.getApplied()), res == null ? null : res.getTotalAmount()))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CardOperationResponse>> debitWithdrawal(String id, Mono<DebitWithdrawalRequest> req, ServerWebExchange exchange) {
        log.info("POST /api/cards/{}/debit/withdrawals - retiro con débito", id);
        return req.flatMap(r -> service.debitWithdrawal(id, r))
                .doOnSuccess(res -> log.info("Retiro aplicado? {} - total={}", res != null && Boolean.TRUE.equals(res.getApplied()), res == null ? null : res.getTotalAmount()))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<CardOperationResponse>> payCreditWithDebitCard(String id, Mono<PayCreditRequest> req, ServerWebExchange exchange) {
        log.info("POST /api/cards/{}/debit/pay-credit - pago de crédito con débito", id);
        return req.flatMap(r -> service.payCreditWithDebitCard(id, r))
                .doOnSuccess(res -> log.info("Pago de crédito con débito aplicado? {}", res != null && Boolean.TRUE.equals(res.getApplied())))
                .map(ResponseEntity::ok);
    }
    
    @Override
    public Mono<ResponseEntity<CardOperationResponse>> debitDeposit(String id,
                                                                    Mono<DebitDepositRequest> debitDepositRequest,
                                                                    ServerWebExchange exchange) {
        log.info("POST /api/cards/{}/debit/deposits - deposit", id);
        return debitDepositRequest
            .flatMap(req -> service.debitDeposit(id, req))
            .map(ResponseEntity::ok);
    }

    // Queries
    @Override
    public Mono<ResponseEntity<Flux<CardMovement>>> getCardMovements(String id, Integer limit, ServerWebExchange exchange) {
        log.info("GET /api/cards/{}/movements?limit={} - últimos movimientos", id, limit);
        return Mono.just(ResponseEntity.ok(service.lastMovements(id, limit)));
    }

    @Override
    public Mono<ResponseEntity<PrimaryAccountBalance>> getPrimaryAccountBalance(String id, ServerWebExchange exchange) {
        log.info("GET /api/cards/{}/primary-account/balance - saldo principal", id);
        return service.primaryBalance(id).map(ResponseEntity::ok);
    }
}
