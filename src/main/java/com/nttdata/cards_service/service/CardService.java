package com.nttdata.cards_service.service;

import com.nttdata.cards_service.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CardService {

    //Registrar una nueva tarjeta (Si el cliente tiene deudas vencidas, se rechaza)
    Mono<CardResponse> createCard(CardRequest request);
    //Listar todas las tarjetas
    Flux<CardResponse> listCards();
    //Obtener una tarjeta por su id
    Mono<CardResponse> getCardById(String id);
    //Actualizar tarjeta
    Mono<CardResponse> updateCard(String id, CardRequest request);
    //Eliminar tarjeta
    Mono<Void> deleteCard(String id);
    //Reemplaza las cuentas asociadas y define la principal
    Mono<CardResponse> replaceAccounts(String id, CardAccountsReplaceRequest body);
    //Agrega una cuenta a la tarjeta
    Mono<CardResponse> addAccount(String id, AddAccountRequest body);
    //Elimina una cuenta asociada
    Mono<CardResponse> removeAccount(String id, String accountId);
    //Reordena prioridad de cuentas
    Mono<CardResponse> reorderAccounts(String id, AccountReorderRequest body);
    //Cambia la cuenta principal
    Mono<CardResponse> setPrimaryAccount(String id, SetPrimaryAccountRequest body);
    //Pago con débito(compras)
    Mono<CardOperationResponse> debitPayment(String id, DebitPaymentRequest req);
    //Retiro con débito(ATM-Retiro Caj)
    Mono<CardOperationResponse> debitWithdrawal(String id, DebitWithdrawalRequest req);
    //Pagar un crédito con la tajeta de débito
    Mono<CardOperationResponse> payCreditWithDebitCard(String id, PayCreditRequest req);
    //últimos movimientos
    Flux<CardMovement> lastMovements(String id, Integer limit);
    //Saldo de la cuenta principal asociada
    Mono<PrimaryAccountBalance> primaryBalance(String id);

}
