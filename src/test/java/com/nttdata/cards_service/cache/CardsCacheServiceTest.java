package com.nttdata.cards_service.cache;

import com.nttdata.cards_service.model.CardMovement;
import com.nttdata.cards_service.model.CardResponse;
import com.nttdata.cards_service.model.PrimaryAccountBalance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import static com.nttdata.cards_service.cache.CacheKeys.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardsCacheServiceTest {

  @Mock ReactiveRedisTemplate<String, CardResponse> cardRedis;
  @Mock ReactiveRedisTemplate<String, PrimaryAccountBalance> pbRedis;
  @Mock ReactiveRedisTemplate<String, CardMovement[]> movRedis;

  @Mock ReactiveValueOperations<String, CardResponse> cardOps;
  @Mock ReactiveValueOperations<String, PrimaryAccountBalance> pbOps;
  @Mock ReactiveValueOperations<String, CardMovement[]> movOps;

  @InjectMocks CardsCacheService service;

  @BeforeEach
  void init() {
    when(cardRedis.opsForValue()).thenReturn(cardOps);
    when(pbRedis.opsForValue()).thenReturn(pbOps);
    when(movRedis.opsForValue()).thenReturn(movOps);
    ReflectionTestUtils.setField(service, "cardByIdTtl", Duration.ofMinutes(5));
    ReflectionTestUtils.setField(service, "primaryBalanceTtl", Duration.ofSeconds(30));
    ReflectionTestUtils.setField(service, "movementsTtl", Duration.ofSeconds(45));
  }

  @Test
  void cardById_cacheMiss_thenStoresAndReturns() {
    String key = CARD_BY_ID + ":C1";
    when(cardOps.get(key)).thenReturn(Mono.empty());
    when(cardOps.set(eq(key), any(CardResponse.class), any(Duration.class))).thenReturn(Mono.just(true));

    AtomicInteger calls = new AtomicInteger(0);
    Supplier<Mono<CardResponse>> loader = () -> {
      calls.incrementAndGet();
      return Mono.just(new CardResponse().id("C1"));
    };
    StepVerifier.create(service.cardById("C1", loader))
        .expectNextMatches(cr -> "C1".equals(cr.getId()))
        .verifyComplete();

    verify(cardOps).get(key);
    verify(cardOps).set(eq(key), any(CardResponse.class), eq(Duration.ofMinutes(5)));
    org.junit.jupiter.api.Assertions.assertEquals(1, calls.get());
  }

  @Test
  void primaryBalance_cacheHit() {
    String key = PRIMARY_BALANCE + ":C1";
    when(pbOps.get(key)).thenReturn(Mono.just(new PrimaryAccountBalance().cardId("C1").accountId("A1").balance(10.0)));

    StepVerifier.create(service.primaryBalance("C1", Mono::empty))
        .expectNextMatches(pb -> "C1".equals(pb.getCardId()) && "A1".equals(pb.getAccountId()) && pb.getBalance() == 10.0)
        .verifyComplete();

    verify(pbOps).get(key);
    verify(pbOps, never()).set(anyString(), any(), any());
  }
}
