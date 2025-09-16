package com.nttdata.cards_service.service;

import com.nttdata.cards_service.cache.CardsCacheService;
import com.nttdata.cards_service.integration.transactions.TransactionsClient;
import com.nttdata.cards_service.integration.transactions.dto.TxGet;
import com.nttdata.cards_service.integration.transactions.dto.TxProduct;
import com.nttdata.cards_service.model.CardMovement;
import com.nttdata.cards_service.model.entity.Card;
import com.nttdata.cards_service.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MovementQueryServiceTest {

  @Test
  void lastMovements_debit_sorts_and_maps_sender() {
    CardRepository repo = mock(CardRepository.class);
    TransactionsClient tx = mock(TransactionsClient.class);
    CardsCacheService cache = mock(CardsCacheService.class);

    Card card = new Card();
    card.setId("C1");
    card.setCardType("DEBIT");
    card.setPrimaryAccountId("A1");
    card.getAccounts().add("A2");

    when(repo.findById("C1")).thenReturn(Mono.just(card));
    when(cache.movements(eq("C1"), eq(3), any())).thenAnswer(inv -> ((Supplier<Flux<CardMovement>>) inv.getArgument(2)).get());

    TxGet t1 = Mockito.mock(TxGet.class);
    when(t1.getId()).thenReturn("T1");
    when(t1.getType()).thenReturn("purchase");
    when(t1.getAmount()).thenReturn(10.0);
    when(t1.getCreatedDate()).thenReturn(OffsetDateTime.now().minusMinutes(1));
    TxProduct s1 = new TxProduct(); s1.setId("A1"); s1.setType("savings_account"); s1.setNumber("001");
    when(t1.getSender()).thenReturn(s1);

    TxGet t2 = Mockito.mock(TxGet.class);
    when(t2.getId()).thenReturn("T2");
    when(t2.getType()).thenReturn("purchase");
    when(t2.getAmount()).thenReturn(20.0);
    when(t2.getCreatedDate()).thenReturn(OffsetDateTime.now());
    TxProduct s2 = new TxProduct(); s2.setId("A2"); s2.setType("savings_account"); s2.setNumber("002");
    when(t2.getSender()).thenReturn(s2);

    when(tx.findByProduct("A1")).thenReturn(Flux.just(t1));
    when(tx.findByProduct("A2")).thenReturn(Flux.just(t2));

    MovementQueryService svc = new MovementQueryService(repo, tx, cache);

    StepVerifier.create(svc.lastMovements("C1", 3))
        .expectNextMatches(cm -> "T2".equals(cm.getId()) && cm.getAmount() == 20.0)
        .expectNextMatches(cm -> "T1".equals(cm.getId()) && cm.getAmount() == 10.0)
        .verifyComplete();
  }

  @Test
  void lastMovements_credit_maps_receiver() {
    CardRepository repo = mock(CardRepository.class);
    TransactionsClient tx = mock(TransactionsClient.class);
    CardsCacheService cache = mock(CardsCacheService.class);

    Card card = new Card();
    card.setId("C2");
    card.setCardType("CREDIT");
    card.setCreditId("CR1");

    when(repo.findById("C2")).thenReturn(Mono.just(card));
    when(cache.movements(eq("C2"), eq(2), any())).thenAnswer(inv -> ((Supplier<Flux<CardMovement>>) inv.getArgument(2)).get());

    TxGet t1 = Mockito.mock(TxGet.class);
    when(t1.getId()).thenReturn("T1"); when(t1.getType()).thenReturn("payment");
    when(t1.getAmount()).thenReturn(50.0);
    when(t1.getCreatedDate()).thenReturn(OffsetDateTime.now());
    TxProduct r1 = new TxProduct(); r1.setId("CR1"); r1.setType("personal_credit"); r1.setNumber("CRED-01");
    when(t1.getReceiver()).thenReturn(r1);

    when(tx.findByProduct("CR1")).thenReturn(Flux.just(t1));

    MovementQueryService svc = new MovementQueryService(repo, tx, cache);

    StepVerifier.create(svc.lastMovements("C2", 2))
        .expectNextMatches(cm -> "T1".equals(cm.getId()) && cm.getAmount() == 50.0)
        .verifyComplete();
  }
}
