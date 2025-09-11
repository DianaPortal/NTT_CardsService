package com.nttdata.cards_service.service;

import com.nttdata.cards_service.integration.accounts.dto.*;
import com.nttdata.cards_service.integration.credits.*;
import com.nttdata.cards_service.integration.credits.dto.*;
import com.nttdata.cards_service.integration.transactions.*;
import com.nttdata.cards_service.integration.transactions.dto.*;
import com.nttdata.cards_service.model.*;
import com.nttdata.cards_service.model.entity.*;
import com.nttdata.cards_service.model.value.*;
import lombok.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;
import com.nttdata.cards_service.repository.CardRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PayCreditService {
  private final DebitOrchestratorService debit;
  private final CreditsClient credits;
  private final TransactionsClient tx;
  private final CardRepository cardRepo;

  public Mono<StoredOperation> pay(String cardId, String opId, String creditId, double amount, String note) {
    Map<String, Object> md = Map.of("purpose", "pay-credit", "creditId", creditId);
    // 1) Debitar en cascada (idempotente)
    return cardRepo.findById(cardId)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Card not found")))
        .flatMap(card -> debit.debit(cardId, opId, amount, "PAY_CREDIT", md, "payment")
            // 2) Aplicar pago en Credits (enviamos payerCustomerId + channel=CARD)
            .flatMap(stored -> {
              CreditPaymentRequest req = new CreditPaymentRequest();//CreditPaymentRequest(amount,notes)
              req.setPayerCustomerId(card.getCustomerId());
              req.setChannel("CARD");

              return credits.applyPayment(creditId, req)
                  .then(Mono.defer(() -> {
                    TxPost post = new TxPost();
                    post.setType("payment");
                    post.setAmount(amount);
                    post.setReceiver(TxProduct.of(creditId, "personal_credit")); // o business_credit/credit_card según sea
                    return tx.create(post).thenReturn(stored);
                  }))
                  // 3) Compensación si falla Credits: depositamos de vuelta cada slice y registramos reversa
                  .onErrorResume(ex -> debit.compensate(card, opId, stored.getResult().getSlices(), "payment")
                      .then(Mono.error(new IllegalStateException("Credit payment failed in Credits; funds returned to accounts", ex))));
            })
        );
  }




}
