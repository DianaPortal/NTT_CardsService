package com.nttdata.cards_service.service;

import com.nttdata.cards_service.model.entity.Card;
import com.nttdata.cards_service.model.value.StoredOperation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CardOpsTest {

  @Test
  void findOperation_present_and_absent() {
    Card c = new Card();
    StoredOperation a = new StoredOperation(); a.setId("op1");
    StoredOperation b = new StoredOperation(); b.setId("op2");
    c.getOperations().add(a); c.getOperations().add(b);

    Optional<StoredOperation> found = CardOps.findOperation(c, "op2");
    assertTrue(found.isPresent());
    assertEquals("op2", found.get().getId());

    assertTrue(CardOps.findOperation(c, "nope").isEmpty());
  }

  @Test
  void upsertOperation_dedups_and_keeps_last_n() {
    Card c = new Card();
    for (int i=1;i<=5;i++) {
      StoredOperation op = new StoredOperation(); op.setId("op"+i);
      CardOps.upsertOperation(c, op, 5);
    }
    assertEquals(5, c.getOperations().size());

    // upsert existing moves it to top without duplicates
    StoredOperation op3 = new StoredOperation(); op3.setId("op3");
    CardOps.upsertOperation(c, op3, 5);
    assertEquals(5, c.getOperations().size());
    assertEquals("op3", c.getOperations().get(0).getId());

    // keep only last 3
    StoredOperation op6 = new StoredOperation(); op6.setId("op6");
    CardOps.upsertOperation(c, op6, 3);
    assertEquals(3, c.getOperations().size());
    assertEquals("op6", c.getOperations().get(0).getId());
  }
}