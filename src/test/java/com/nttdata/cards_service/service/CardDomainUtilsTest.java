package com.nttdata.cards_service.service;

import org.junit.jupiter.api.Test;
import com.nttdata.cards_service.model.CardOperationResponseSlices;
import java.util.LinkedHashMap;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CardDomainUtilsTest {

  @Test
  void normalizeAccounts_preservesOrder_primaryFirst_distinct_and_nullsRemoved() {
    var out = CardDomainUtils.normalizeAccounts("A1",
        Arrays.asList("A2", "A3", "A1", null, "A2"));
    assertEquals(3, out.size());
    assertEquals(List.of("A1","A2","A3"), out);
  }

  @Test
  void planSlices_exact_and_multiAccounts() {
    var balances = new LinkedHashMap<String, Double>();
    balances.put("A1", 30.0);
    balances.put("A2", 50.0);

    List<CardOperationResponseSlices> plan = CardDomainUtils.planSlices(60.0, balances);
    assertEquals(2, plan.size());
    assertEquals("A1", plan.get(0).getAccountId());
    assertEquals(30.0, plan.get(0).getAmount(), 0.0001);
    assertEquals("A2", plan.get(1).getAccountId());
    assertEquals(30.0, plan.get(1).getAmount(), 0.0001);
  }

  @Test
  void planSlices_throws_when_insufficient() {
    var balances = new LinkedHashMap<String, Double>();
    balances.put("A1", 10.0);
    balances.put("A2", 5.0);
    assertThrows(IllegalStateException.class, () -> CardDomainUtils.planSlices(20.1, balances));
  }
}
