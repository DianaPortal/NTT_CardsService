package com.nttdata.cards_service.service;

import com.nttdata.cards_service.cache.CardsCacheService;
import com.nttdata.cards_service.integration.accounts.AccountsClient;
import com.nttdata.cards_service.integration.accounts.dto.AccountDto;
import com.nttdata.cards_service.model.PrimaryAccountBalance;
import com.nttdata.cards_service.model.entity.Card;
import com.nttdata.cards_service.repository.CardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.function.Supplier;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrimaryBalanceServiceTest {

  @Mock CardRepository repo;
  @Mock AccountsClient accounts;
  @Mock CardsCacheService cache;

  @InjectMocks PrimaryBalanceService service;

  @Test
  void get_retornaSaldoPrimario_yUsaCacheLoader() {
    Card card = new Card(); card.setId("C1"); card.setPrimaryAccountId("A1");
    when(repo.findById("C1")).thenReturn(Mono.just(card));
    AccountDto acc = new AccountDto(); acc.setId("A1"); acc.setBalance(123.45);
    when(accounts.getAccount("A1")).thenReturn(Mono.just(acc));

    // el cache delega al loader y retorna el resultado
    when(cache.primaryBalance(anyString(), org.mockito.ArgumentMatchers.any()))
        .thenAnswer(inv -> ((
            Supplier<Mono<PrimaryAccountBalance>>)
            inv.getArgument(1))
            .get());

    StepVerifier.create(service.get("C1"))
        .expectNextMatches(pb -> pb.getBalance() == 123.45 && "A1".equals(pb.getAccountId()))
        .verifyComplete();
  }
}

