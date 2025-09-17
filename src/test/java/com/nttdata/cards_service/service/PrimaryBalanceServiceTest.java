package com.nttdata.cards_service.service;


import com.nttdata.cards_service.integration.accounts.*;
import com.nttdata.cards_service.integration.accounts.dto.*;
import com.nttdata.cards_service.model.entity.*;
import com.nttdata.cards_service.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import reactor.core.publisher.*;
import reactor.test.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrimaryBalanceServiceTest {

  @Mock
  CardRepository repo;
  @Mock
  AccountsClient accounts;


  @InjectMocks
  PrimaryBalanceService service;

  @Test
  void get_retornaSaldoPrimario() {
    Card card = new Card();
    card.setId("C1");
    card.setPrimaryAccountId("A1");
    when(repo.findById("C1")).thenReturn(Mono.just(card));
    AccountDto acc = new AccountDto();
    acc.setId("A1");
    acc.setBalance(123.45);
    when(accounts.getAccount("A1")).thenReturn(Mono.just(acc));

    StepVerifier.create(service.get("C1"))
        .expectNextMatches(pb -> pb.getBalance() == 123.45 && "A1".equals(pb.getAccountId()))
        .verifyComplete();
  }
}

