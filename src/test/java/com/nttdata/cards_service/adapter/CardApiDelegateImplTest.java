package com.nttdata.cards_service.adapter;


import com.nttdata.cards_service.model.*;
import com.nttdata.cards_service.service.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;
import reactor.test.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CardApiDelegateImplTest {

  private CardApiDelegateImpl delegate(CardService service) {
    return new CardApiDelegateImpl(service);
  }

  // POST /api/cards
  @Test
  void registerCard_created_201_with_body() {
    CardService svc = mock(CardService.class);
    CardRequest req = new CardRequest().customerId("CU1");
    CardResponse res = new CardResponse().id("C1");
    when(svc.createCard(any())).thenReturn(Mono.just(res));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.registerCard(Mono.just(req), null))
        .assertNext(resp -> {
          assertEquals(201, resp.getStatusCodeValue());
          assertNotNull(resp.getBody());
          assertEquals("C1", resp.getBody().getId());
        })
        .verifyComplete();

    ArgumentCaptor<CardRequest> cap = ArgumentCaptor.forClass(CardRequest.class);
    verify(svc).createCard(cap.capture());
    assertEquals("CU1", cap.getValue().getCustomerId());
  }

  @Test
  void registerCard_empty_completes_without_value() {
    CardService svc = mock(CardService.class);
    when(svc.createCard(any())).thenReturn(Mono.empty());

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.registerCard(Mono.just(new CardRequest()), null))
        .verifyComplete();
  }

  @Test
  void registerCard_error_propagates() {
    CardService svc = mock(CardService.class);
    when(svc.createCard(any())).thenReturn(Mono.error(new RuntimeException("boom")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.registerCard(Mono.just(new CardRequest()), null))
        .expectErrorMessage("boom")
        .verify();
  }

  // GET /api/cards
  @Test
  void listCards_ok_wraps_flux() {
    CardService svc = mock(CardService.class);
    when(svc.listCards()).thenReturn(Flux.just(new CardResponse().id("C1"), new CardResponse().id("C2")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.listCards(null))
        .assertNext(resp -> {
          assertEquals(200, resp.getStatusCodeValue());
          StepVerifier.create(resp.getBody())
              .expectNextMatches(cr -> "C1".equals(cr.getId()))
              .expectNextMatches(cr -> "C2".equals(cr.getId()))
              .verifyComplete();
        })
        .verifyComplete();
  }

  // GET /api/cards/{id}
  @Test
  void getCardById_ok_200() {
    CardService svc = mock(CardService.class);
    when(svc.getCardById("C1")).thenReturn(Mono.just(new CardResponse().id("C1")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.getCardById("C1", null))
        .assertNext(resp -> {
          assertEquals(200, resp.getStatusCodeValue());
          assertEquals("C1", resp.getBody().getId());
        })
        .verifyComplete();
  }

  @Test
  void getCardById_empty_completes_without_value() {
    CardService svc = mock(CardService.class);
    when(svc.getCardById("X")).thenReturn(Mono.empty());

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.getCardById("X", null))
        .verifyComplete();
  }

  @Test
  void getCardById_error_propagates() {
    CardService svc = mock(CardService.class);
    when(svc.getCardById("X")).thenReturn(Mono.error(new IllegalStateException("not found")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.getCardById("X", null))
        .expectErrorMessage("not found")
        .verify();
  }

  // PUT /api/cards/{id}
  @Test
  void updateCard_ok_200() {
    CardService svc = mock(CardService.class);
    CardResponse out = new CardResponse().id("C9");
    when(svc.updateCard(eq("C9"), any())).thenReturn(Mono.just(out));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.updateCard("C9", Mono.just(new CardRequest()), null))
        .assertNext(resp -> {
          assertEquals(200, resp.getStatusCodeValue());
          assertEquals("C9", resp.getBody().getId());
        })
        .verifyComplete();
  }

  @Test
  void updateCard_empty_completes_without_value() {
    CardService svc = mock(CardService.class);
    when(svc.updateCard(eq("C9"), any())).thenReturn(Mono.empty());

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.updateCard("C9", Mono.just(new CardRequest()), null))
        .verifyComplete();
  }

  @Test
  void updateCard_error_propagates() {
    CardService svc = mock(CardService.class);
    when(svc.updateCard(eq("C9"), any())).thenReturn(Mono.error(new RuntimeException("bad")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.updateCard("C9", Mono.just(new CardRequest()), null))
        .expectErrorMessage("bad")
        .verify();
  }

  // DELETE /api/cards/{id}
  @Test
  void deleteCard_noContent_204() {
    CardService svc = mock(CardService.class);
    when(svc.deleteCard("C1")).thenReturn(Mono.empty());

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.deleteCard("C1", null))
        .assertNext(resp -> assertEquals(204, resp.getStatusCodeValue()))
        .verifyComplete();

    verify(svc).deleteCard("C1");
  }

  @Test
  void deleteCard_error_propagates() {
    CardService svc = mock(CardService.class);
    when(svc.deleteCard("C1")).thenReturn(Mono.error(new RuntimeException("boom")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.deleteCard("C1", null))
        .expectErrorMessage("boom")
        .verify();
  }

  // PUT /api/cards/{id}/accounts (replace)
  @Test
  void replaceCardAccounts_ok() {
    CardService svc = mock(CardService.class);
    CardResponse out = new CardResponse().id("C1").primaryAccountId("A1");
    when(svc.replaceAccounts(eq("C1"), any())).thenReturn(Mono.just(out));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.replaceCardAccounts("C1", Mono.just(new CardAccountsReplaceRequest()), null))
        .assertNext(resp -> {
          assertEquals(200, resp.getStatusCodeValue());
          assertEquals("C1", resp.getBody().getId());
        })
        .verifyComplete();
  }

  @Test
  void replaceCardAccounts_empty_completes() {
    CardService svc = mock(CardService.class);
    when(svc.replaceAccounts(eq("C1"), any())).thenReturn(Mono.empty());

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.replaceCardAccounts("C1", Mono.just(new CardAccountsReplaceRequest()), null))
        .verifyComplete();
  }

  @Test
  void replaceCardAccounts_error() {
    CardService svc = mock(CardService.class);
    when(svc.replaceAccounts(eq("C1"), any())).thenReturn(Mono.error(new RuntimeException("x")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.replaceCardAccounts("C1", Mono.just(new CardAccountsReplaceRequest()), null))
        .expectErrorMessage("x")
        .verify();
  }

  // POST /api/cards/{id}/accounts (add)
  @Test
  void addCardAccount_ok() {
    CardService svc = mock(CardService.class);
    when(svc.addAccount(eq("C1"), any())).thenReturn(Mono.just(new CardResponse().id("C1")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.addCardAccount("C1", Mono.just(new AddAccountRequest().accountId("A2")), null))
        .assertNext(resp -> assertEquals(200, resp.getStatusCodeValue()))
        .verifyComplete();
  }

  @Test
  void addCardAccount_error() {
    CardService svc = mock(CardService.class);
    when(svc.addAccount(eq("C1"), any())).thenReturn(Mono.error(new IllegalArgumentException("dup")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.addCardAccount("C1", Mono.just(new AddAccountRequest().accountId("A2")), null))
        .expectErrorMessage("dup")
        .verify();
  }

  // DELETE /api/cards/{id}/accounts/{accountId}
  @Test
  void removeCardAccount_ok() {
    CardService svc = mock(CardService.class);
    when(svc.removeAccount("C1", "A2")).thenReturn(Mono.just(new CardResponse().id("C1")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.removeCardAccount("C1", "A2", null))
        .assertNext(resp -> assertEquals(200, resp.getStatusCodeValue()))
        .verifyComplete();
  }

  @Test
  void removeCardAccount_error() {
    CardService svc = mock(CardService.class);
    when(svc.removeAccount("C1", "A2")).thenReturn(Mono.error(new RuntimeException("nope")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.removeCardAccount("C1", "A2", null))
        .expectErrorMessage("nope")
        .verify();
  }

  // PUT /api/cards/{id}/accounts/reorder
  @Test
  void reorderCardAccounts_ok() {
    CardService svc = mock(CardService.class);
    when(svc.reorderAccounts(eq("C1"), any())).thenReturn(Mono.just(new CardResponse().id("C1")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.reorderCardAccounts("C1", Mono.just(new AccountReorderRequest()), null))
        .assertNext(resp -> assertEquals(200, resp.getStatusCodeValue()))
        .verifyComplete();
  }

  @Test
  void reorderCardAccounts_empty_and_error() {
    CardService svc = mock(CardService.class);
    when(svc.reorderAccounts(eq("C1"), any()))
        .thenReturn(Mono.empty())
        .thenReturn(Mono.error(new RuntimeException("bad")));

    CardApiDelegateImpl d = delegate(svc);

    StepVerifier.create(d.reorderCardAccounts("C1", Mono.just(new AccountReorderRequest()), null))
        .verifyComplete();

    StepVerifier.create(d.reorderCardAccounts("C1", Mono.just(new AccountReorderRequest()), null))
        .expectErrorMessage("bad")
        .verify();
  }

  // PUT /api/cards/{id}/primary-account
  @Test
  void setPrimaryAccount_ok_empty_error() {
    CardService svc = mock(CardService.class);
    when(svc.setPrimaryAccount(eq("C1"), any()))
        .thenReturn(Mono.just(new CardResponse().id("C1")))
        .thenReturn(Mono.empty())
        .thenReturn(Mono.error(new RuntimeException("bad")));

    CardApiDelegateImpl d = delegate(svc);

    StepVerifier.create(d.setPrimaryAccount("C1", Mono.just(new SetPrimaryAccountRequest().accountId("A1")), null))
        .assertNext(resp -> assertEquals(200, resp.getStatusCodeValue()))
        .verifyComplete();

    StepVerifier.create(d.setPrimaryAccount("C1", Mono.just(new SetPrimaryAccountRequest().accountId("A1")), null))
        .verifyComplete();

    StepVerifier.create(d.setPrimaryAccount("C1", Mono.just(new SetPrimaryAccountRequest().accountId("A1")), null))
        .expectErrorMessage("bad")
        .verify();
  }

  // POST /api/cards/{id}/debit/payments
  @Test
  void debitPayment_applied_true_false_and_empty() {
    CardService svc = mock(CardService.class);
    CardOperationResponse ok = new CardOperationResponse().applied(true).totalAmount(10.0);
    CardOperationResponse ko = new CardOperationResponse().applied(false).totalAmount(10.0);

    when(svc.debitPayment(eq("C1"), any()))
        .thenReturn(Mono.just(ok))
        .thenReturn(Mono.just(ko))
        .thenReturn(Mono.empty());

    CardApiDelegateImpl d = delegate(svc);
    DebitPaymentRequest req = new DebitPaymentRequest().operationId("OP1").amount(10.0);

    // applied true
    StepVerifier.create(d.debitPayment("C1", Mono.just(req), null))
        .assertNext(resp -> {
          assertEquals(200, resp.getStatusCodeValue());
          assertEquals(Boolean.TRUE, resp.getBody().getApplied());
        })
        .verifyComplete();

    // applied false
    StepVerifier.create(d.debitPayment("C1", Mono.just(req), null))
        .assertNext(resp -> assertNotEquals(Boolean.TRUE, resp.getBody().getApplied()))
        .verifyComplete();

    // empty
    StepVerifier.create(d.debitPayment("C1", Mono.just(req), null))
        .verifyComplete();
  }

  @Test
  void debitPayment_error() {
    CardService svc = mock(CardService.class);
    when(svc.debitPayment(eq("C1"), any())).thenReturn(Mono.error(new RuntimeException("err")));
    CardApiDelegateImpl d = delegate(svc);

    StepVerifier.create(d.debitPayment("C1", Mono.just(new DebitPaymentRequest().operationId("OP")), null))
        .expectErrorMessage("err")
        .verify();
  }

  // POST /api/cards/{id}/debit/withdrawals
  @Test
  void debitWithdrawal_applied_true_false_empty_error() {
    CardService svc = mock(CardService.class);
    CardOperationResponse ok = new CardOperationResponse().applied(true);
    CardOperationResponse ko = new CardOperationResponse().applied(false);

    when(svc.debitWithdrawal(eq("C1"), any()))
        .thenReturn(Mono.just(ok))
        .thenReturn(Mono.just(ko))
        .thenReturn(Mono.empty());

    CardApiDelegateImpl d = delegate(svc);
    DebitWithdrawalRequest req = new DebitWithdrawalRequest().operationId("OP2").amount(5.0);

    StepVerifier.create(d.debitWithdrawal("C1", Mono.just(req), null))
        .assertNext(resp -> assertEquals(Boolean.TRUE, resp.getBody().getApplied()))
        .verifyComplete();

    StepVerifier.create(d.debitWithdrawal("C1", Mono.just(req), null))
        .assertNext(resp -> assertNotEquals(Boolean.TRUE, resp.getBody().getApplied()))
        .verifyComplete();

    StepVerifier.create(d.debitWithdrawal("C1", Mono.just(req), null))
        .verifyComplete();

    // error
    when(svc.debitWithdrawal(eq("C1"), any())).thenReturn(Mono.error(new RuntimeException("w")));
    StepVerifier.create(d.debitWithdrawal("C1", Mono.just(req), null))
        .expectErrorMessage("w")
        .verify();
  }

  // POST /api/cards/{id}/debit/pay-credit
  @Test
  void payCreditWithDebitCard_applied_true_false_empty_error() {
    CardService svc = mock(CardService.class);
    CardOperationResponse ok = new CardOperationResponse().applied(true);
    CardOperationResponse ko = new CardOperationResponse().applied(false);

    when(svc.payCreditWithDebitCard(eq("C1"), any()))
        .thenReturn(Mono.just(ok))
        .thenReturn(Mono.just(ko))
        .thenReturn(Mono.empty());

    CardApiDelegateImpl d = delegate(svc);
    PayCreditRequest req = new PayCreditRequest().operationId("OP3").creditId("CR1").amount(20.0);

    StepVerifier.create(d.payCreditWithDebitCard("C1", Mono.just(req), null))
        .assertNext(resp -> assertEquals(Boolean.TRUE, resp.getBody().getApplied()))
        .verifyComplete();

    StepVerifier.create(d.payCreditWithDebitCard("C1", Mono.just(req), null))
        .assertNext(resp -> assertNotEquals(Boolean.TRUE, resp.getBody().getApplied()))
        .verifyComplete();

    StepVerifier.create(d.payCreditWithDebitCard("C1", Mono.just(req), null))
        .verifyComplete();

    when(svc.payCreditWithDebitCard(eq("C1"), any())).thenReturn(Mono.error(new RuntimeException("p")));
    StepVerifier.create(d.payCreditWithDebitCard("C1", Mono.just(req), null))
        .expectErrorMessage("p")
        .verify();
  }

  // GET /api/cards/{id}/movements
  @Test
  void getCardMovements_ok_wraps_flux() {
    CardService svc = mock(CardService.class);
    when(svc.lastMovements("C1", 5)).thenReturn(Flux.just(new CardMovement().id("M1"), new CardMovement().id("M2")));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.getCardMovements("C1", 5, null))
        .assertNext(resp -> {
          assertEquals(200, resp.getStatusCodeValue());
          StepVerifier.create(resp.getBody())
              .expectNextMatches(m -> "M1".equals(m.getId()))
              .expectNextMatches(m -> "M2".equals(m.getId()))
              .verifyComplete();
        })
        .verifyComplete();
  }

  // GET /api/cards/{id}/primary-account/balance
  @Test
  void getPrimaryAccountBalance_ok_200_and_error() {
    CardService svc = mock(CardService.class);
    when(svc.primaryBalance("C1")).thenReturn(Mono.just(new PrimaryAccountBalance().cardId("C1").balance(123.0)));

    CardApiDelegateImpl d = delegate(svc);
    StepVerifier.create(d.getPrimaryAccountBalance("C1", null))
        .assertNext(resp -> {
          assertEquals(200, resp.getStatusCodeValue());
          assertEquals("C1", resp.getBody().getCardId());
          assertEquals(123.0, resp.getBody().getBalance(), 0.0001);
        })
        .verifyComplete();

    when(svc.primaryBalance("C1")).thenReturn(Mono.error(new RuntimeException("pb")));
    StepVerifier.create(d.getPrimaryAccountBalance("C1", null))
        .expectErrorMessage("pb")
        .verify();
  }
}
