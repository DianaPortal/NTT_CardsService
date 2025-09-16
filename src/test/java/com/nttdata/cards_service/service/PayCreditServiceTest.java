package com.nttdata.cards_service.service;

import com.nttdata.cards_service.integration.transactions.*;
import com.nttdata.cards_service.integration.transactions.dto.*;
import com.nttdata.cards_service.model.*;
import com.nttdata.cards_service.model.entity.*;
import com.nttdata.cards_service.model.value.*;
import com.nttdata.cards_service.repository.*;
import org.junit.jupiter.api.*;
import reactor.core.publisher.*;
import reactor.test.*;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PayCreditServiceTest {

  @Test
  void pay_success_applies_credit_and_tx() {
    DebitOrchestratorService debit = mock(DebitOrchestratorService.class);
    com.nttdata.cards_service.integration.credits.CreditClient credits = mock(com.nttdata.cards_service.integration.credits.CreditClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);
    CardRepository repo = mock(CardRepository.class);

    Card card = new Card();
    card.setId("C1");
    card.setCustomerId("CU1");
    when(repo.findById("C1")).thenReturn(Mono.just(card));

    StoredOperation stored = new StoredOperation();
    CardOperationResponse res = new CardOperationResponse();
    res.setSlices(List.of(new CardOperationResponseSlices()));
    stored.setResult(res);
    when(debit.debit(eq("C1"), eq("OP1"), eq(100.0), anyString(), anyMap(), eq("payment")))
        .thenReturn(Mono.just(stored));

    when(credits.applyPayment(eq("CR1"), any())).thenReturn(Mono.empty());
    when(tx.create(any(TxPost.class))).thenReturn(Mono.just(new TxGet()));

    PayCreditService svc = new PayCreditService(debit, credits, tx, repo);

    StepVerifier.create(svc.pay("C1", "OP1", "CR1", 100.0, "nota"))
        .expectNext(stored)
        .verifyComplete();

    verify(credits).applyPayment(eq("CR1"), argThat(req -> "CARD".equals(req.getChannel()) && req.getAmount() == 100.0));
    verify(tx).create(any(TxPost.class));
  }

  @Test
  void pay_failure_in_credits_triggers_compensation() {
    DebitOrchestratorService debit = mock(DebitOrchestratorService.class);
    com.nttdata.cards_service.integration.credits.CreditClient credits = mock(com.nttdata.cards_service.integration.credits.CreditClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);
    CardRepository repo = mock(CardRepository.class);

    Card card = new Card();
    card.setId("C1");
    card.setCustomerId("CU1");
    when(repo.findById("C1")).thenReturn(Mono.just(card));

    StoredOperation stored = new StoredOperation();
    CardOperationResponse res = new CardOperationResponse();
    CardOperationResponseSlices s = new CardOperationResponseSlices();
    s.setAccountId("A1");
    s.setAmount(50.0);
    res.setSlices(List.of(s));
    stored.setResult(res);
    when(debit.debit(eq("C1"), eq("OP2"), eq(50.0), anyString(), anyMap(), eq("payment")))
        .thenReturn(Mono.just(stored));

    when(credits.applyPayment(eq("CR1"), any())).thenReturn(Mono.error(new RuntimeException("boom")));
    when(debit.compensate(eq(card), eq("OP2"), anyList(), eq("payment"))).thenReturn(Mono.empty());

    PayCreditService svc = new PayCreditService(debit, credits, tx, repo);

    StepVerifier.create(svc.pay("C1", "OP2", "CR1", 50.0, "nota"))
        .expectError(IllegalStateException.class)
        .verify();

    verify(debit).compensate(eq(card), eq("OP2"), anyList(), eq("payment"));
  }
}
