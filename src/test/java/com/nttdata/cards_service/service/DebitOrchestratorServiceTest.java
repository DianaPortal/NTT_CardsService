package com.nttdata.cards_service.service;

import com.nttdata.cards_service.integration.accounts.*;
import com.nttdata.cards_service.integration.accounts.dto.*;
import com.nttdata.cards_service.integration.transactions.*;
import com.nttdata.cards_service.integration.transactions.dto.*;
import com.nttdata.cards_service.model.*;
import com.nttdata.cards_service.model.entity.*;
import com.nttdata.cards_service.model.value.*;
import com.nttdata.cards_service.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import reactor.core.publisher.*;
import reactor.test.*;

import java.time.*;
import java.util.*;

import static com.nttdata.cards_service.model.CardResponse.StatusEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DebitOrchestratorServiceTest {

  @Test
  void debit_happyPath_slicesAcrossAccounts_and_persists() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);

    Card card = new Card();
    card.setId("C1");
    card.setCardType("DEBIT");
    card.setStatus(ACTIVE);
    card.setPrimaryAccountId("A1");
    card.setAccounts(List.of("A2"));
    card.setCreationDate(OffsetDateTime.now());

    when(repo.findById("C1")).thenReturn(Mono.just(card));
    when(repo.save(any(Card.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    AccountDto a1 = new AccountDto();
    a1.setId("A1");
    a1.setBalance(30.0);
    a1.setActive(true);
    AccountDto a2 = new AccountDto();
    a2.setId("A2");
    a2.setBalance(50.0);
    a2.setActive(true);
    when(accounts.getAccount("A1")).thenReturn(Mono.just(a1));
    when(accounts.getAccount("A2")).thenReturn(Mono.just(a2));

    BalanceOperationResponse r1 = new BalanceOperationResponse();
    r1.setCommissionApplied(0.5);
    BalanceOperationResponse r2 = new BalanceOperationResponse();
    r2.setCommissionApplied(0.0);
    when(accounts.applyBalanceOperation(eq("A1"), any(BalanceOperationRequest.class))).thenReturn(Mono.just(r1));
    when(accounts.applyBalanceOperation(eq("A2"), any(BalanceOperationRequest.class))).thenReturn(Mono.just(r2));

    when(tx.create(any(TxPost.class))).thenReturn(Mono.just(new TxGet()));

    DebitOrchestratorService svc = new DebitOrchestratorService(repo, accounts, tx);

    StepVerifier.create(svc.debit("C1", "OP1", 40.0, "DEBIT_PAYMENT", Map.of("k", "v"), "purchase"))
        .assertNext(stored -> {
          assertEquals("OP1", stored.getId());
          CardOperationResponse res = stored.getResult();
          assertTrue(res.getApplied());
          assertEquals(40.0, res.getTotalAmount(), 0.0001);
          assertEquals(0.5, res.getCommissionTotal(), 0.0001);
          assertEquals(2, res.getSlices().size());
          assertEquals("A1", res.getSlices().get(0).getAccountId());
          assertEquals(30.0, res.getSlices().get(0).getAmount(), 0.0001);
          assertEquals("A2", res.getSlices().get(1).getAccountId());
          assertEquals(10.0, res.getSlices().get(1).getAmount(), 0.0001);
        })
        .verifyComplete();

    // verificamos que se registraron 2 transacciones (una por slice)
    verify(tx, times(2)).create(any(TxPost.class));
    ArgumentCaptor<Card> saved = ArgumentCaptor.forClass(Card.class);
    verify(repo).save(saved.capture());
    assertEquals(1, saved.getValue().getOperations().size());
  }

  @Test
  void debit_fails_when_not_debit_or_not_active() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);
    DebitOrchestratorService svc = new DebitOrchestratorService(repo, accounts, tx);

    Card c1 = new Card();
    c1.setId("X");
    c1.setCardType("CREDIT");
    when(repo.findById("X")).thenReturn(Mono.just(c1));
    StepVerifier.create(svc.debit("X", "OP", 10, "K", Map.of(), "purchase"))
        .expectError(IllegalStateException.class).verify();

    Card c2 = new Card();
    c2.setId("Y");
    c2.setCardType("DEBIT");
    c2.setStatus(ACTIVE);
    when(repo.findById("Y")).thenReturn(Mono.just(c2));
    StepVerifier.create(svc.debit("Y", "OP", 10, "K", Map.of(), "purchase"))
        .expectError(IllegalStateException.class).verify();
  }

  @Test
  void debit_fails_when_insufficient_balance() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);

    Card card = new Card();
    card.setId("C3");
    card.setCardType("DEBIT");
    card.setStatus(ACTIVE);
    card.setPrimaryAccountId("A1");

    when(repo.findById("C3")).thenReturn(Mono.just(card));
    when(accounts.getAccount("A1")).thenReturn(Mono.just(new AccountDto() {{
      setId("A1");
      setBalance(5.0);
      setActive(true);
    }}));

    DebitOrchestratorService svc = new DebitOrchestratorService(repo, accounts, tx);

    StepVerifier.create(svc.debit("C3", "OP", 10.0, "K", Map.of(), "purchase"))
        .expectError(IllegalStateException.class).verify();
  }

  @Test
  void debit_cardNotFound_throws() {
    CardRepository repo = mock(CardRepository.class);
    when(repo.findById("NOPE")).thenReturn(Mono.empty());

    DebitOrchestratorService svc = new DebitOrchestratorService(repo, mock(AccountsClient.class), mock(TransactionsClient.class));

    StepVerifier.create(svc.debit("NOPE", "OP", 10.0, "K", Map.of(), "purchase"))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void debit_notActive_throws() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);

    Card c = new Card();
    c.setId("C1");
    c.setCardType("DEBIT");
    c.setStatus(CardResponse.StatusEnum.BLOCKED); // no ACTIVE

    when(repo.findById("C1")).thenReturn(Mono.just(c));

    DebitOrchestratorService svc = new DebitOrchestratorService(repo, accounts, tx);

    StepVerifier.create(svc.debit("C1", "OP", 10.0, "K", Map.of(), "purchase"))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Card is not ACTIVE")))
        .verify();
  }

  @Test
  void debit_withdrawal_limit_exceeded_throws_and_not_for_purchase() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);

    Card card = new Card();
    card.setId("C4");
    card.setCardType("DEBIT");
    card.setStatus(CardResponse.StatusEnum.ACTIVE);
    card.setPrimaryAccountId("A1");
    card.setAccounts(List.of("A1"));
    CardRequestLimits limits = new CardRequestLimits();
    limits.setAtmWithdrawalLimit(100.0);
    card.setLimits(limits);

    when(repo.findById("C4")).thenReturn(Mono.just(card));
    AccountDto a1 = new AccountDto();
    a1.setId("A1");
    a1.setBalance(500.0);
    a1.setActive(true);
    when(accounts.getAccount("A1")).thenReturn(Mono.just(a1));
    when(repo.save(any(Card.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    DebitOrchestratorService svc = new DebitOrchestratorService(repo, accounts, tx);

    // withdrawal supera límite -> error
    StepVerifier.create(svc.debit("C4", "OPW", 150.0, "WITHDRAW", Map.of(), "withdrawal"))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("ATM withdrawal limit exceeded")))
        .verify();

    // purchase ignora límite ATM -> OK hasta tx (simulamos devolviendo respuestas)
    when(accounts.applyBalanceOperation(eq("A1"), any(BalanceOperationRequest.class)))
        .thenReturn(Mono.just(new BalanceOperationResponse()));
    when(tx.create(any(TxPost.class))).thenReturn(Mono.just(new TxGet()));

    StepVerifier.create(svc.debit("C4", "OPP", 150.0, "PURCHASE", Map.of("foo", "bar"), "purchase"))
        .expectNextMatches(op -> "OPP".equals(op.getId()) && op.getResult().getApplied())
        .verifyComplete();
  }

  @Test
  void debit_commissionNull_is_treated_as_zero_and_metadata_and_index_are_propagated() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);

    Card card = new Card();
    card.setId("C5");
    card.setCardType("DEBIT");
    card.setStatus(CardResponse.StatusEnum.ACTIVE);
    card.setPrimaryAccountId("A1");
    card.setAccounts(List.of("A1"));

    when(repo.findById("C5")).thenReturn(Mono.just(card));
    AccountDto a1 = new AccountDto();
    a1.setId("A1");
    a1.setBalance(80.0);
    a1.setActive(true);
    when(accounts.getAccount("A1")).thenReturn(Mono.just(a1));

    // comisión null -> debe mapear 0.0
    when(accounts.applyBalanceOperation(eq("A1"), any(BalanceOperationRequest.class)))
        .thenReturn(Mono.just(new BalanceOperationResponse() {{
          setCommissionApplied(null);
        }}));
    when(tx.create(any(TxPost.class))).thenReturn(Mono.just(new TxGet()));
    when(repo.save(any(Card.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    DebitOrchestratorService svc = new DebitOrchestratorService(repo, accounts, tx);

    Map<String, Object> metadata = Map.of("k", "v");
    StepVerifier.create(svc.debit("C5", "OP1", 20.0, "DEBIT_PAYMENT", metadata, "purchase"))
        .assertNext(stored -> {
          assertEquals("OP1", stored.getId());
          assertEquals(1, stored.getResult().getSlices().size());
          assertEquals(0.0, stored.getResult().getSlices().get(0).getCommissionApplied(), 0.0001);
          assertEquals(20.0, stored.getResult().getTotalAmount(), 0.0001);
        })
        .verifyComplete();

    // capturar y verificar metadata y operationId indexado #0
    ArgumentCaptor<BalanceOperationRequest> cap = ArgumentCaptor.forClass(BalanceOperationRequest.class);
    verify(accounts).applyBalanceOperation(eq("A1"), cap.capture());
    BalanceOperationRequest sent = cap.getValue();
    assertEquals("OP1#0", sent.getOperationId());
    assertEquals("withdrawal", sent.getType()); // el servicio usa "withdrawal" para el movimiento en cuenta
    assertEquals(20.0, sent.getAmount(), 0.0001);
    assertEquals(metadata, sent.getMetadata());

    ArgumentCaptor<TxPost> txCap = ArgumentCaptor.forClass(TxPost.class);
    verify(tx).create(txCap.capture());
    TxPost post = txCap.getValue();
    assertEquals("purchase", post.getType());
    assertEquals(20.0, post.getAmount(), 0.0001);
    assertNotNull(post.getSender());
    assertEquals("A1", post.getSender().getId());
  }

  @Test
  void debit_idempotent_returns_existing_without_calling_downstreams() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);

    Card card = new Card();
    card.setId("C6");
    card.setCardType("DEBIT");
    card.setStatus(CardResponse.StatusEnum.ACTIVE);
    card.setPrimaryAccountId("A1");
    // operación ya almacenada
    StoredOperation stored = new StoredOperation();
    stored.setId("OPX");
    CardOperationResponse res = new CardOperationResponse();
    res.setApplied(true);
    res.setMessage("PREV");
    stored.setResult(res);
    card.getOperations().add(stored);

    when(repo.findById("C6")).thenReturn(Mono.just(card));

    DebitOrchestratorService svc = new DebitOrchestratorService(repo, accounts, tx);

    StepVerifier.create(svc.debit("C6", "OPX", 10.0, "ANY", Map.of(), "purchase"))
        .expectNextMatches(s -> "PREV".equals(s.getResult().getMessage()))
        .verifyComplete();

    verifyNoInteractions(accounts);
    verifyNoInteractions(tx);
  }

  @Test
  void compensate_deposits_each_slice_and_registers_reversals() {
    AccountsClient accounts = mock(AccountsClient.class);
    TransactionsClient tx = mock(TransactionsClient.class);
    CardRepository repo = mock(CardRepository.class);

    when(accounts.applyBalanceOperation(anyString(), any(BalanceOperationRequest.class)))
        .thenReturn(Mono.just(new BalanceOperationResponse()));
    when(tx.create(any(TxPost.class))).thenReturn(Mono.just(new TxGet()));

    DebitOrchestratorService svc = new DebitOrchestratorService(repo, accounts, tx);

    Card card = new Card();
    card.setId("C7");
    var s1 = new com.nttdata.cards_service.model.CardOperationResponseSlices();
    s1.setAccountId("A1");
    s1.setAmount(15.0);
    var s2 = new com.nttdata.cards_service.model.CardOperationResponseSlices();
    s2.setAccountId("A2");
    s2.setAmount(25.0);

    StepVerifier.create(svc.compensate(card, "OPC", List.of(s1, s2), "payment"))
        .verifyComplete();

    // se llaman dos depósitos con ids indexados y tipo deposit
    ArgumentCaptor<BalanceOperationRequest> cap = ArgumentCaptor.forClass(BalanceOperationRequest.class);
    InOrder inOrder = inOrder(accounts, tx);
    inOrder.verify(accounts).applyBalanceOperation(eq("A1"), cap.capture());
    inOrder.verify(tx).create(any(TxPost.class));
    inOrder.verify(accounts).applyBalanceOperation(eq("A2"), cap.capture());
    inOrder.verify(tx).create(any(TxPost.class));

    List<BalanceOperationRequest> reqs = cap.getAllValues();
    assertEquals("OPC#comp#0", reqs.get(0).getOperationId());
    assertEquals("deposit", reqs.get(0).getType());
    assertEquals(15.0, reqs.get(0).getAmount(), 0.0001);
    assertEquals("OPC#comp#1", reqs.get(1).getOperationId());
    assertEquals(25.0, reqs.get(1).getAmount(), 0.0001);

    // verificar reversals
    ArgumentCaptor<TxPost> txCap = ArgumentCaptor.forClass(TxPost.class);
    verify(tx, times(2)).create(txCap.capture());
    List<TxPost> posts = txCap.getAllValues();
    assertEquals("reversal", posts.get(0).getType());
    assertEquals(15.0, posts.get(0).getAmount(), 0.0001);
    assertNotNull(posts.get(0).getReceiver());
    assertEquals("A1", posts.get(0).getReceiver().getId());

    assertEquals("reversal", posts.get(1).getType());
    assertEquals(25.0, posts.get(1).getAmount(), 0.0001);
    assertEquals("A2", posts.get(1).getReceiver().getId());
  }
}
