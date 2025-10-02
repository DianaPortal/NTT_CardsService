package com.nttdata.cards_service.service.impl;


import com.nttdata.cards_service.cache.*;
import com.nttdata.cards_service.integration.accounts.*;
import com.nttdata.cards_service.integration.accounts.dto.*;
import com.nttdata.cards_service.integration.credits.*;
import com.nttdata.cards_service.integration.credits.dto.*;
import com.nttdata.cards_service.model.*;
import com.nttdata.cards_service.model.entity.*;
import com.nttdata.cards_service.model.value.*;
import com.nttdata.cards_service.repository.*;
import com.nttdata.cards_service.service.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import reactor.core.publisher.*;
import reactor.test.*;
import com.nttdata.cards_service.adapter.mapper.CardMapper;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openapitools.jackson.nullable.JsonNullable.of;
import static reactor.core.publisher.Mono.just;

class CardServiceImplTest {

  private CardServiceImpl service(CardRepository repo,
                                  CreditClient credits,
                                  DebitOrchestratorService debit,
                                  PayCreditService pay,
                                  MovementQueryService mov,
                                  PrimaryBalanceService primary,
                                  com.nttdata.cards_service.adapter.mapper.CardMapper mapper,
                                  CardsCacheService cache,
                                  AccountsClient accounts) {
    return new CardServiceImpl(repo, credits, debit, pay, mov, primary, mapper, cache, accounts);
  }

  // --------- createCard ---------

  @Test
  void createCard_rejects_when_client_has_overdue() {
    CardRepository repo = mock(CardRepository.class);
    CreditClient credits = mock(CreditClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    OverdueRes od = new OverdueRes();
    od.setHasOverdue(true);
    when(credits.hasOverdue("CUST1")).thenReturn(just(od));

    CardServiceImpl svc = service(repo, credits, null, null, null, null, mapper, null, null);

    CardRequest req = new CardRequest().customerId("CUST1");
    StepVerifier.create(svc.createCard(req))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Client has overdue debts")))
        .verify();
  }

  @Test
  void createCard_debit_requires_primary() {
    CardRepository repo = mock(CardRepository.class);
    CreditClient credits = mock(CreditClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    when(credits.hasOverdue(anyString())).thenReturn(Mono.empty()); // default false
    Card entity = new Card();
    entity.setCardType("DEBIT");
    entity.setPrimaryAccountId(null);
    when(mapper.toEntity(any(CardRequest.class))).thenReturn(entity);

    CardServiceImpl svc = service(repo, credits, null, null, null, null, mapper, null, null);

    StepVerifier.create(svc.createCard(new CardRequest().customerId("CUST1")))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("DEBIT card requires primaryAccountId")))
        .verify();
  }

  @Test
  void createCard_debit_primary_not_found() {
    CardRepository repo = mock(CardRepository.class);
    CreditClient credits = mock(CreditClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);
    AccountsClient accounts = mock(AccountsClient.class);

    when(credits.hasOverdue(anyString())).thenReturn(Mono.empty());
    Card entity = new Card();
    entity.setCardType("DEBIT");
    entity.setPrimaryAccountId("A1");
    entity.setCustomerId("CUST1");
    entity.setAccounts(new ArrayList<>());

    when(mapper.toEntity(any())).thenReturn(entity);

    when(accounts.getAccount("A1")).thenReturn(Mono.empty());

    CardServiceImpl svc = service(repo, credits, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.createCard(new CardRequest().customerId("CUST1")))
        .expectError(IllegalArgumentException.class)
        .verify();

  }

  @Test
  void createCard_debit_primary_inactive() {
    CardRepository repo = mock(CardRepository.class);
    CreditClient credits = mock(CreditClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);
    AccountsClient accounts = mock(AccountsClient.class);

    when(credits.hasOverdue(anyString())).thenReturn(Mono.empty());
    Card entity = new Card();
    entity.setCardType("DEBIT");
    entity.setPrimaryAccountId("A1");
    entity.setAccounts(List.of("A2", "A1", "A2"));
    when(mapper.toEntity(any())).thenReturn(entity);

    AccountDto acc = new AccountDto();
    acc.setId("A1");
    acc.setActive(false);
    when(accounts.getAccount("A1")).thenReturn(just(acc));

    CardServiceImpl svc = service(repo, credits, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.createCard(new CardRequest().customerId("CUST1")))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Primary account is not active")))
        .verify();
  }

  @Test
  void createCard_debit_success_normalizes_and_saves() {
    CardRepository repo = mock(CardRepository.class);
    CreditClient credits = mock(CreditClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);
    AccountsClient accounts = mock(AccountsClient.class);

    when(credits.hasOverdue(anyString())).thenReturn(Mono.empty());
    Card entity = new Card();
    entity.setId("C1");
    entity.setCardType("DEBIT");
    entity.setPrimaryAccountId("A1");
    entity.setAccounts(List.of("A2", "A1", "A3", "A2"));
    when(mapper.toEntity(any())).thenReturn(entity);

    AccountDto acc = new AccountDto();
    acc.setId("A1");
    acc.setActive(true);
    when(accounts.getAccount("A1")).thenReturn(just(acc));

    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C1"));

    CardServiceImpl svc = service(repo, credits, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.createCard(new CardRequest().customerId("CU1")))
        .expectNextMatches(r -> "C1".equals(r.getId()))
        .verifyComplete();

    ArgumentCaptor<Card> cap = ArgumentCaptor.forClass(Card.class);
    verify(repo).save(cap.capture());
    // A1 primero, sin duplicados
    assertEquals(List.of("A1", "A2", "A3"), cap.getValue().getAccounts());
    assertNotNull(cap.getValue().getCreationDate());
    assertNotNull(cap.getValue().getUpdatedDate());
  }

  @Test
  void createCard_credit_requires_creditId() {
    CardRepository repo = mock(CardRepository.class);
    CreditClient credits = mock(CreditClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    when(credits.hasOverdue(anyString())).thenReturn(Mono.empty());
    Card entity = new Card();
    entity.setCardType("CREDIT");
    entity.setCreditId(null);
    when(mapper.toEntity(any())).thenReturn(entity);

    CardServiceImpl svc = service(repo, credits, null, null, null, null, mapper, null, null);

    StepVerifier.create(svc.createCard(new CardRequest().customerId("CUST1")))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("CREDIT card requires creditId")))
        .verify();
  }

  @Test
  void createCard_credit_success() {
    CardRepository repo = mock(CardRepository.class);
    CreditClient credits = mock(CreditClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    when(credits.hasOverdue(anyString())).thenReturn(Mono.empty());
    CardRequest req = new CardRequest();
    req.setId("C2");
    req.setCardType(CardRequest.CardTypeEnum.CREDIT);
    req.setCreditId(of("CR1"));
    req.setCustomerId("CUST1");
    when(mapper.toEntity(any(CardRequest.class))).thenAnswer(inv -> {
      CardRequest r = inv.getArgument(0);
      Card c = new Card();
      c.setCardType(r.getCardType() != null ? r.getCardType().name() : null);
      c.setCreditId(r.getCreditId() != null ? r.getCreditId().orElse(null) : null);
      c.setId(r.getId());
      c.setCustomerId(r.getCustomerId());
      c.setAccounts(new ArrayList<>());
      c.setCreationDate(OffsetDateTime.now());
      c.setUpdatedDate(OffsetDateTime.now());
      return c;
    });


    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C2"));

    CardServiceImpl svc = service(repo, credits, null, null, null, null, mapper,
        mock(CardsCacheService.class), mock(AccountsClient.class));

    StepVerifier.create(svc.createCard(req))
        .expectNextMatches(r -> "C2".equals(r.getId()))
        .verifyComplete();
  }

  // --------- list/get/update/delete ---------

  @Test
  void listCards_maps_all() {
    CardRepository repo = mock(CardRepository.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    when(repo.findAll()).thenReturn(Flux.just(new Card(), new Card()));
    when(mapper.toResponse(any(Card.class)))
        .thenReturn(new CardResponse().id("1"), new CardResponse().id("2"));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, null);

    StepVerifier.create(svc.listCards())
        .expectNextMatches(r -> "1".equals(r.getId()))
        .expectNextMatches(r -> "2".equals(r.getId()))
        .verifyComplete();
  }

  @Test
  void getCardById_uses_cache_supplier() {
    CardRepository repo = mock(CardRepository.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);
    CardsCacheService cache = mock(CardsCacheService.class);

    when(repo.findById("X")).thenReturn(just(new Card()));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("X"));

    when(cache.cardById(eq("X"), any()))
        .thenAnswer(inv -> {
          Supplier<Mono<CardResponse>> sup = inv.getArgument(1);
          return sup.get();
        });

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, cache, null);

    StepVerifier.create(svc.getCardById("X"))
        .expectNextMatches(r -> "X".equals(r.getId()))
        .verifyComplete();
  }

  @Test
  void updateCard_debit_primary_not_found() {
    CardRepository repo = mock(CardRepository.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);
    AccountsClient accounts = mock(AccountsClient.class);

    Card db = new Card();
    db.setId("C1");
    db.setCreationDate(OffsetDateTime.now().minusDays(1));
    when(repo.findById("C1")).thenReturn(just(db));

    Card upd = new Card();
    upd.setCardType("DEBIT");
    upd.setPrimaryAccountId("A2");
    when(mapper.toEntity(any())).thenReturn(upd);

    when(accounts.getAccount("A2")).thenReturn(Mono.empty());

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.updateCard("C1", new CardRequest()))
        .expectError(IllegalArgumentException.class)
        .verify();
  }

  @Test
  void updateCard_debit_primary_inactive() {
    CardRepository repo = mock(CardRepository.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);
    AccountsClient accounts = mock(AccountsClient.class);

    when(repo.findById("C1")).thenReturn(just(new Card()));
    Card upd = new Card();
    upd.setCardType("DEBIT");
    upd.setPrimaryAccountId("A2");
    when(mapper.toEntity(any())).thenReturn(upd);

    AccountDto acc = new AccountDto();
    acc.setId("A2");
    acc.setActive(false);
    when(accounts.getAccount("A2")).thenReturn(just(acc));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.updateCard("C1", new CardRequest()))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Primary account is not active")))
        .verify();
  }

  @Test
  void updateCard_debit_primary_valid_saves_normalized() {
    CardRepository repo = mock(CardRepository.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);
    AccountsClient accounts = mock(AccountsClient.class);

    Card db = new Card();
    db.setId("C10");
    db.setCreationDate(OffsetDateTime.now().minusDays(3));
    when(repo.findById("C10")).thenReturn(just(db));

    Card upd = new Card();
    upd.setCardType("DEBIT");
    upd.setPrimaryAccountId("A1");
    upd.setAccounts(List.of("A3", "A1", "A2", "A2"));
    when(mapper.toEntity(any())).thenReturn(upd);

    AccountDto acc = new AccountDto();
    acc.setId("A1");
    acc.setActive(true);
    when(accounts.getAccount("A1")).thenReturn(just(acc));

    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C10"));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.updateCard("C10", new CardRequest()))
        .expectNextMatches(r -> "C10".equals(r.getId()))
        .verifyComplete();

    ArgumentCaptor<Card> cap = ArgumentCaptor.forClass(Card.class);
    verify(repo).save(cap.capture());
    assertEquals(List.of("A1", "A3", "A2"), cap.getValue().getAccounts());
    assertEquals(db.getCreationDate(), cap.getValue().getCreationDate());
    assertNotNull(cap.getValue().getUpdatedDate());
  }

  @Test
  void updateCard_non_debit_or_no_primary_flows_through() {
    CardRepository repo = mock(CardRepository.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    when(repo.findById("C2")).thenReturn(just(new Card()));
    Card upd = new Card();
    upd.setCardType("CREDIT"); // or DEBIT with null primary
    when(mapper.toEntity(any())).thenReturn(upd);

    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C2"));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, null);

    StepVerifier.create(svc.updateCard("C2", new CardRequest()))
        .expectNextMatches(r -> "C2".equals(r.getId()))
        .verifyComplete();
  }

  @Test
  void deleteCard_deletes() {
    CardRepository repo = mock(CardRepository.class);
    when(repo.deleteById("Z")).thenReturn(Mono.empty());

    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, null);

    StepVerifier.create(svc.deleteCard("Z")).verifyComplete();
    verify(repo).deleteById("Z");
  }

  // --------- replace/add/remove/reorder/setPrimary ---------

  @Test
  void replaceAccounts_not_debit() {
    CardRepository repo = mock(CardRepository.class);
    Card c = new Card();
    c.setCardType("CREDIT");
    when(repo.findById("C1")).thenReturn(just(c));

    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, null);

    StepVerifier.create(svc.replaceAccounts("C1", new CardAccountsReplaceRequest()))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Not a DEBIT card")))
        .verify();
  }

  @Test
  void replaceAccounts_accounts_required_and_must_include_primary() {
    CardRepository repo = mock(CardRepository.class);
    Card c = new Card();
    c.setCardType("DEBIT");
    when(repo.findById("C1")).thenReturn(just(c));

    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, null);

    // accounts required
    StepVerifier.create(svc.replaceAccounts("C1", new CardAccountsReplaceRequest()))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("accounts required")))
        .verify();

    // primary must be included
    CardAccountsReplaceRequest body = new CardAccountsReplaceRequest()
        .primaryAccountId("A1")
        .accounts(List.of("A2"));
    StepVerifier.create(svc.replaceAccounts("C1", body))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("primaryAccountId must be included")))
        .verify();
  }

  @Test
  void replaceAccounts_success_validates_and_saves() {


    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    AccountDto acc1 = new AccountDto();
    acc1.setId("A1");
    AccountDto acc2 = new AccountDto();
    acc2.setId("A2");
    Card c = new Card();
    c.setId("C1");
    c.setCardType("DEBIT");
    when(repo.findById("C1")).thenReturn(just(c));
    when(accounts.getAccount("A1")).thenReturn(just(acc1));
    when(accounts.getAccount("A2")).thenReturn(just(acc2));
    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C1"));
    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);

    CardAccountsReplaceRequest body = new CardAccountsReplaceRequest()
        .primaryAccountId("A1")
        .accounts(List.of("A2", "A1", "A2"));
    StepVerifier.create(svc.replaceAccounts("C1", body))
        .expectNextMatches(r -> "C1".equals(r.getId()))
        .verifyComplete();

    ArgumentCaptor<Card> cap = ArgumentCaptor.forClass(Card.class);
    verify(repo).save(cap.capture());
    assertEquals(List.of("A1", "A2"), cap.getValue().getAccounts());
  }

  @Test
  void addAccount_not_debit_or_errors() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    // Not DEBIT
    Card c = new Card();
    c.setCardType("CREDIT");
    when(repo.findById("C1")).thenReturn(just(c));
    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);
    StepVerifier.create(svc.addAccount("C1", new AddAccountRequest().accountId("A9")))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Not a DEBIT card")))
        .verify();

    // Account not found
    Card d = new Card();
    d.setCardType("DEBIT");
    when(repo.findById("C2")).thenReturn(just(d));
    when(accounts.getAccount("A1")).thenReturn(Mono.empty());
    StepVerifier.create(svc.addAccount("C2", new AddAccountRequest().accountId("A1")))
        .expectError(IllegalArgumentException.class).verify();

    AccountDto acc = new AccountDto();
    acc.setId("A2");
    acc.setActive(false);
    // Account inactive
    when(accounts.getAccount("A2")).thenReturn(just((acc)));
    StepVerifier.create(svc.addAccount("C2", new AddAccountRequest().accountId("A2")))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Account is not active")))
        .verify();
  }

  @Test
  void addAccount_success_adds_when_absent() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    Card c = new Card();
    c.setId("C3");
    c.setCardType("DEBIT");
    c.setPrimaryAccountId("A0");
    c.setAccounts(new ArrayList<>(List.of("A0")));
    when(repo.findById("C3")).thenReturn(just(c));

    AccountDto acc = new AccountDto();
    acc.setId("A3");
    acc.setActive(true);

    when(accounts.getAccount("A3")).thenReturn(just((acc)));
    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C3"));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.addAccount("C3", new AddAccountRequest().accountId("A3")))
        .expectNextMatches(r -> "C3".equals(r.getId()))
        .verifyComplete();

    ArgumentCaptor<Card> cap = ArgumentCaptor.forClass(Card.class);
    verify(repo).save(cap.capture());
    assertEquals(List.of("A0", "A3"), cap.getValue().getAccounts());
  }

  @Test
  void removeAccount_validations_and_success() {
    CardRepository repo = mock(CardRepository.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    // Not DEBIT
    Card c1 = new Card();
    c1.setCardType("CREDIT");
    when(repo.findById("C1")).thenReturn(just(c1));
    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, null);
    StepVerifier.create(svc.removeAccount("C1", "A1"))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Not a DEBIT card")))
        .verify();

    // Cannot remove primary
    Card c2 = new Card();
    c2.setCardType("DEBIT");
    c2.setPrimaryAccountId("A1");
    c2.setAccounts(new ArrayList<>(List.of("A1", "A2")));
    when(repo.findById("C2")).thenReturn(just(c2));
    StepVerifier.create(svc.removeAccount("C2", "A1"))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Cannot remove primary account")))
        .verify();

    // Success
    Card c3 = new Card();
    c3.setId("C3");
    c3.setCardType("DEBIT");
    c3.setPrimaryAccountId("A1");
    c3.setAccounts(new ArrayList<>(List.of("A1", "A2", "A3")));
    when(repo.findById("C3")).thenReturn(just(c3));
    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C3"));

    StepVerifier.create(svc.removeAccount("C3", "A2"))
        .expectNextMatches(r -> "C3".equals(r.getId()))
        .verifyComplete();
  }

  @Test
  void reorderAccounts_validations_and_success() {
    CardRepository repo = mock(CardRepository.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    // Not DEBIT
    Card c1 = new Card();
    c1.setCardType("CREDIT");
    when(repo.findById("C1")).thenReturn(just(c1));
    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, null);
    StepVerifier.create(svc.reorderAccounts("C1", new AccountReorderRequest()))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Not a DEBIT card")))
        .verify();

    // ids required
    Card c2 = new Card();
    c2.setCardType("DEBIT");
    c2.setPrimaryAccountId("A1");
    when(repo.findById("C2")).thenReturn(just(c2));
    StepVerifier.create(svc.reorderAccounts("C2", new AccountReorderRequest()))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("accountIds required")))
        .verify();

    // must include primary
    StepVerifier.create(svc.reorderAccounts("C2", new AccountReorderRequest().accountIds(List.of("A2", "A3"))))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("primary account must be included")))
        .verify();

    // success
    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C2"));
    StepVerifier.create(svc.reorderAccounts("C2", new AccountReorderRequest().accountIds(List.of("A1", "A3", "A2", "A2"))))
        .expectNextMatches(r -> "C2".equals(r.getId()))
        .verifyComplete();
  }

  @Test
  void setPrimaryAccount_validations_and_success() {

    AccountDto acc = new AccountDto();
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    com.nttdata.cards_service.adapter.mapper.CardMapper mapper = mock(com.nttdata.cards_service.adapter.mapper.CardMapper.class);

    // Not DEBIT
    Card c1 = new Card();
    c1.setCardType("CREDIT");
    when(repo.findById("C1")).thenReturn(just(c1));
    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);
    StepVerifier.create(svc.setPrimaryAccount("C1", new SetPrimaryAccountRequest().accountId("A1")))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Not a DEBIT card")))
        .verify();

    // Not associated
    Card c2 = new Card();
    c2.setCardType("DEBIT");
    c2.setAccounts(List.of("A2", "A3"));
    when(repo.findById("C2")).thenReturn(just(c2));
    StepVerifier.create(svc.setPrimaryAccount("C2", new SetPrimaryAccountRequest().accountId("A1")))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Account not associated to the card")))
        .verify();

    // Account not found
    Card c3 = new Card();
    c3.setCardType("DEBIT");
    c3.setAccounts(List.of("A1", "A2"));
    when(repo.findById("C3")).thenReturn(just(c3));
    when(accounts.getAccount("A1")).thenReturn(Mono.empty());
    StepVerifier.create(svc.setPrimaryAccount("C3", new SetPrimaryAccountRequest().accountId("A1")))
        .expectError(IllegalArgumentException.class).verify();

    // Inactive
    acc.setId("A1");
    acc.setActive(false);
    when(accounts.getAccount("A1")).thenReturn(just(acc));
    StepVerifier.create(svc.setPrimaryAccount("C3", new SetPrimaryAccountRequest().accountId("A1")))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Account is not active")))
        .verify();

    // Success
    when(accounts.getAccount("A1")).thenReturn(just(new AccountDto() {{
      setId("A1");
      setActive(true);
    }}));
    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C3"));
    StepVerifier.create(svc.setPrimaryAccount("C3", new SetPrimaryAccountRequest().accountId("A1")))
        .expectNextMatches(r -> "C3".equals(r.getId()))
        .verifyComplete();
  }

  // --------- debit/withdraw/pay and reads ---------

  @Test
  void debitPayment_delegates_to_debit_orchestrator() {
    DebitOrchestratorService debit = mock(DebitOrchestratorService.class);
    StoredOperation stored = new StoredOperation();
    stored.setResult(new CardOperationResponse().message("OK"));
    when(debit.debit(eq("C1"), eq("OP1"), eq(100.0), anyString(), anyMap(), eq("purchase"))).thenReturn(just(stored));

    CardServiceImpl svc = service(null, null, debit, null, null, null, null, null, null);

    DebitPaymentRequest req = new DebitPaymentRequest().operationId("OP1").amount(100.0).channel("POS").merchant("M1");
    StepVerifier.create(svc.debitPayment("C1", req))
        .expectNextMatches(r -> "OK".equals(r.getMessage()))
        .verifyComplete();
  }

  @Test
  void debitWithdrawal_delegates() {
    DebitOrchestratorService debit = mock(DebitOrchestratorService.class);
    StoredOperation stored = new StoredOperation();
    stored.setResult(new CardOperationResponse().message("OKW"));
    when(debit.debit(eq("C1"), eq("OP2"), eq(50.0), anyString(), anyMap(), eq("withdrawal"))).thenReturn(just(stored));

    CardServiceImpl svc = service(null, null, debit, null, null, null, null, null, null);

    DebitWithdrawalRequest req = new DebitWithdrawalRequest().operationId("OP2").amount(50.0).channel("ATM").atmId("ATM1");
    StepVerifier.create(svc.debitWithdrawal("C1", req))
        .expectNextMatches(r -> "OKW".equals(r.getMessage()))
        .verifyComplete();
  }

  @Test
  void payCreditWithDebitCard_delegates() {
    PayCreditService pay = mock(PayCreditService.class);
    StoredOperation stored = new StoredOperation();
    stored.setResult(new CardOperationResponse().message("PAID"));
    when(pay.pay(eq("C1"), eq("OP3"), eq("CR1"), eq(25.0), eq("note"))).thenReturn(just(stored));


    CardServiceImpl svc = service(null, null, null, pay, null, null, null, null, null);

    PayCreditRequest req = new PayCreditRequest().operationId("OP3").creditId("CR1").amount(25.0).note("note");
    StepVerifier.create(svc.payCreditWithDebitCard("C1", req))
        .expectNextMatches(r -> "PAID".equals(r.getMessage()))
        .verifyComplete();
  }

  @Test
  void lastMovements_limits_are_normalized() {
    MovementQueryService mov = mock(MovementQueryService.class);

    // limit null -> 10
    when(mov.lastMovements("C1", 10)).thenReturn(Flux.just(new CardMovement().id("M1")));
    CardServiceImpl svc = service(null, null, null, null, mov, null, null, null, null);
    StepVerifier.create(svc.lastMovements("C1", null))
        .expectNextMatches(m -> "M1".equals(m.getId()))
        .verifyComplete();

    // limit > 100 -> 100
    when(mov.lastMovements("C1", 100)).thenReturn(Flux.just(new CardMovement().id("M2")));
    StepVerifier.create(svc.lastMovements("C1", 1000))
        .expectNextMatches(m -> "M2".equals(m.getId()))
        .verifyComplete();
  }

  @Test
  void primaryBalance_delegates() {
    PrimaryBalanceService primary = mock(PrimaryBalanceService.class);
    when(primary.get("C1")).thenReturn(just(new PrimaryAccountBalance().cardId("C1").balance(10.0)));

    CardServiceImpl svc = service(null, null, null, null, null, primary, null, null, null);

    StepVerifier.create(svc.primaryBalance("C1"))
        .expectNextMatches(p -> "C1".equals(p.getCardId()) && p.getBalance() == 10.0)
        .verifyComplete();
  }

  @Test
  void updateCard_debit_with_null_primary_account_flows_without_validation() {
    CardRepository repo = mock(CardRepository.class);
    CardMapper mapper = mock(CardMapper.class);
    AccountsClient accounts = mock(AccountsClient.class);
    Card db = new Card();
    db.setId("C55");
    db.setCreationDate(OffsetDateTime.now().minusDays(2));
    when(repo.findById("C55")).thenReturn(just(db));

    Card upd = new Card();
    upd.setCardType("DEBIT");
    upd.setPrimaryAccountId(null); // should skip validation
    upd.setAccounts(new ArrayList<>(List.of("AX", "AY")));
    when(mapper.toEntity(any())).thenReturn(upd);
    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenAnswer(inv -> new CardResponse().id(inv.getArgument(0, Card.class).getId()));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.updateCard("C55", new CardRequest()))
        .expectNextMatches(r -> "C55".equals(r.getId()))
        .verifyComplete();

    verify(accounts, never()).getAccount(anyString());
  }

  @Test
  void addAccount_does_not_duplicate_existing() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    CardMapper mapper = mock(CardMapper.class);

    Card c = new Card();
    c.setId("C7");
    c.setCardType("DEBIT");
    c.setPrimaryAccountId("A1");
    c.setAccounts(new ArrayList<>(List.of("A1", "A2")));
    when(repo.findById("C7")).thenReturn(just(c));

    AccountDto acc = new AccountDto();
    acc.setId("A2"); // already present
    acc.setActive(true);
    when(accounts.getAccount("A2")).thenReturn(just(acc));
    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C7"));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.addAccount("C7", new AddAccountRequest().accountId("A2")))
        .expectNextMatches(r -> "C7".equals(r.getId()))
        .verifyComplete();

    ArgumentCaptor<Card> cap = ArgumentCaptor.forClass(Card.class);
    verify(repo).save(cap.capture());
    assertEquals(List.of("A1", "A2"), cap.getValue().getAccounts());
  }

  @Test
  void addAccount_allows_account_with_null_active() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    CardMapper mapper = mock(CardMapper.class);

    Card c = new Card();
    c.setId("C8");
    c.setCardType("DEBIT");
    c.setPrimaryAccountId("P1");
    c.setAccounts(new ArrayList<>(List.of("P1")));
    when(repo.findById("C8")).thenReturn(just(c));

    AccountDto acc = new AccountDto();
    acc.setId("A9");
    acc.setActive(null); // treated as active
    when(accounts.getAccount("A9")).thenReturn(just(acc));
    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("C8"));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.addAccount("C8", new AddAccountRequest().accountId("A9")))
        .expectNextMatches(r -> "C8".equals(r.getId()))
        .verifyComplete();

    ArgumentCaptor<Card> cap = ArgumentCaptor.forClass(Card.class);
    verify(repo).save(cap.capture());
    assertEquals(List.of("P1", "A9"), cap.getValue().getAccounts());
  }

  @Test
  void setPrimaryAccount_reorders_accounts() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    CardMapper mapper = mock(CardMapper.class);

    Card c = new Card();
    c.setId("CC1");
    c.setCardType("DEBIT");
    c.setPrimaryAccountId("A1");
    c.setAccounts(new ArrayList<>(List.of("A1", "A2", "A3")));
    when(repo.findById("CC1")).thenReturn(just(c));

    AccountDto acc = new AccountDto();
    acc.setId("A3");
    acc.setActive(true);
    when(accounts.getAccount("A3")).thenReturn(just(acc));

    when(repo.save(any(Card.class))).thenAnswer(inv -> just(inv.getArgument(0)));
    when(mapper.toResponse(any(Card.class))).thenReturn(new CardResponse().id("CC1"));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, null, accounts);

    StepVerifier.create(svc.setPrimaryAccount("CC1", new SetPrimaryAccountRequest().accountId("A3")))
        .expectNextMatches(r -> "CC1".equals(r.getId()))
        .verifyComplete();

    ArgumentCaptor<Card> cap = ArgumentCaptor.forClass(Card.class);
    verify(repo).save(cap.capture());
    assertEquals(List.of("A3", "A1", "A2"), cap.getValue().getAccounts());
    assertEquals("A3", cap.getValue().getPrimaryAccountId());
  }

  // -------- debitDeposit tests --------

  @Test
  void debitDeposit_rejects_invalid_amount() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, accounts);

    DebitDepositRequest req = new DebitDepositRequest().operationId("OPX").amount(0d);
    StepVerifier.create(svc.debitDeposit("C1", req))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("amount > 0 requerido")))
        .verify();
  }

  @Test
  void debitDeposit_rejects_missing_operationId() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);
    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, accounts);

    DebitDepositRequest req = new DebitDepositRequest().amount(10d);
    StepVerifier.create(svc.debitDeposit("C1", req))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("operationId requerido")))
        .verify();
  }

  @Test
  void debitDeposit_card_not_found() {
    CardRepository repo = mock(CardRepository.class);
    when(repo.findById("CX")).thenReturn(Mono.empty());
    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, mock(AccountsClient.class));

    DebitDepositRequest req = new DebitDepositRequest().amount(10d).operationId("OP1");
    StepVerifier.create(svc.debitDeposit("CX", req))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Card not found")))
        .verify();
  }

  @Test
  void debitDeposit_rejects_non_debit() {
    CardRepository repo = mock(CardRepository.class);
    Card card = new Card();
    card.setCardType("CREDIT");
    when(repo.findById("CND")).thenReturn(just(card));

    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, mock(AccountsClient.class));

    DebitDepositRequest req = new DebitDepositRequest().amount(10d).operationId("OP1");
    StepVerifier.create(svc.debitDeposit("CND", req))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Solo tarjetas DEBIT")))
        .verify();
  }

  @Test
  void debitDeposit_rejects_not_active_status() {
    CardRepository repo = mock(CardRepository.class);
    Card card = new Card();
    card.setCardType("DEBIT");
    card.setStatus(CardResponse.StatusEnum.EXPIRED);// not ACTIVE
    when(repo.findById("CI")).thenReturn(just(card));

    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, mock(AccountsClient.class));

    DebitDepositRequest req = new DebitDepositRequest().amount(10d).operationId("OP2");
    StepVerifier.create(svc.debitDeposit("CI", req))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Card no ACTIVE")))
        .verify();
  }

  @Test
  void debitDeposit_rejects_missing_primary_account() {
    CardRepository repo = mock(CardRepository.class);
    Card card = new Card();
    card.setCardType("DEBIT");
    card.setStatus(CardResponse.StatusEnum.ACTIVE);
    card.setPrimaryAccountId(null);
    when(repo.findById("CP")).thenReturn(just(card));

    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, mock(AccountsClient.class));
    DebitDepositRequest req = new DebitDepositRequest().amount(10d).operationId("OP3");
    StepVerifier.create(svc.debitDeposit("CP", req))
        .expectErrorSatisfies(ex -> assertTrue(ex.getMessage().contains("Card sin primaryAccount")))
        .verify();
  }

  @Test
  void debitDeposit_success_no_commission_defaults_metadata() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);

    Card card = new Card();
    card.setId("CD1");
    card.setCardType("DEBIT");
    card.setStatus(CardResponse.StatusEnum.ACTIVE);
    card.setPrimaryAccountId("A1");
    when(repo.findById("CD1")).thenReturn(just(card));

    BalanceOperationResponse balResp = new BalanceOperationResponse();
    balResp.setCommissionApplied(null);

    when(accounts.applyBalanceOperation(eq("A1"), any())).thenReturn(just(balResp));

    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, accounts);

    DebitDepositRequest req = new DebitDepositRequest().amount(50d).operationId("OPD1");
    StepVerifier.create(svc.debitDeposit("CD1", req))
        .expectNextMatches(r -> Boolean.TRUE.equals(r.getApplied()) &&
            r.getCommissionTotal() == 0d &&
            "Deposit OK".equals(r.getMessage()) &&
            r.getSlices() != null &&
            r.getSlices().size() == 1 &&
            r.getSlices().get(0).getCommissionApplied() == 0d)
        .verifyComplete();

    ArgumentCaptor<BalanceOperationRequest> cap = ArgumentCaptor.forClass(BalanceOperationRequest.class);
    verify(accounts).applyBalanceOperation(eq("A1"), cap.capture());
    BalanceOperationRequest sent = cap.getValue();
    assertEquals("deposit", sent.getType());
    assertEquals(50d, sent.getAmount());
    assertEquals("INCOME", sent.getMetadata().get("channel"));
    assertEquals("CARD_INCOME", sent.getMetadata().get("description"));
  }

  @Test
  void debitDeposit_success_with_commission_and_custom_metadata() {
    CardRepository repo = mock(CardRepository.class);
    AccountsClient accounts = mock(AccountsClient.class);

    Card card = new Card();
    card.setId("CD2");
    card.setCardType("DEBIT");
    card.setStatus(CardResponse.StatusEnum.ACTIVE);
    card.setPrimaryAccountId("PA1");
    when(repo.findById("CD2")).thenReturn(just(card));

    BalanceOperationResponse balResp = new BalanceOperationResponse();
    balResp.setCommissionApplied(BigDecimal.valueOf(1.25).doubleValue());

    when(accounts.applyBalanceOperation(eq("PA1"), any())).thenReturn(just(balResp));

    CardServiceImpl svc = service(repo, null, null, null, null, null, null, null, accounts);

    DebitDepositRequest req = new DebitDepositRequest()
        .amount(200d)
        .operationId("OPD2")
        .channel("WEB")
        .source("USER_TRANSFER");

    StepVerifier.create(svc.debitDeposit("CD2", req))
        .expectNextMatches(r -> r.getCommissionTotal() == 1.25 &&
            r.getTotalAmount() == 200d &&
            r.getSlices().get(0).getCommissionApplied() == 1.25)
        .verifyComplete();

    ArgumentCaptor<BalanceOperationRequest> cap = ArgumentCaptor.forClass(BalanceOperationRequest.class);
    verify(accounts).applyBalanceOperation(eq("PA1"), cap.capture());
    BalanceOperationRequest sent = cap.getValue();
    assertEquals("WEB", sent.getMetadata().get("channel"));
    assertEquals("USER_TRANSFER", sent.getMetadata().get("description"));
  }

  @Test
  void getCardById_cache_hit_does_not_call_repo_supplier() {
    CardRepository repo = mock(CardRepository.class);
    CardMapper mapper = mock(CardMapper.class);
    CardsCacheService cache = mock(CardsCacheService.class);
    CardResponse cached = new CardResponse().id("CACHED1");
    when(cache.cardById(eq("CACHED1"), any())).thenReturn(just(cached));

    CardServiceImpl svc = service(repo, null, null, null, null, null, mapper, cache, null);

    StepVerifier.create(svc.getCardById("CACHED1"))
        .expectNextMatches(r -> "CACHED1".equals(r.getId()))
        .verifyComplete();

    verify(repo, never()).findById(anyString());
  }
}
