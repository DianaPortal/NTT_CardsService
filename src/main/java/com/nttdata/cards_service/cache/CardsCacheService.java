package com.nttdata.cards_service.cache;


import com.nttdata.cards_service.model.*;
import lombok.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

import java.time.*;
import java.util.function.*;

import static com.nttdata.cards_service.cache.CacheKeys.*;

@Service
@RequiredArgsConstructor
public class CardsCacheService {

  private final ReactiveRedisTemplate<String, CardResponse> cardRedis;
  private final ReactiveRedisTemplate<String, PrimaryAccountBalance> pbRedis;
  private final ReactiveRedisTemplate<String, CardMovement[]> movRedis;

  @Value("${cache.cards.card-by-id.ttl:PT5M}")
  private Duration cardByIdTtl;
  @Value("${cache.cards.primary-balance.ttl:PT30S}")
  private Duration primaryBalanceTtl;
  @Value("${cache.cards.movements.ttl:PT45S}")
  private Duration movementsTtl;

  public Mono<CardResponse> cardById(String cardId, Supplier<Mono<CardResponse>> loader) {
    String key = CARD_BY_ID + ":" + cardId;
    return cardRedis.opsForValue().get(key)
        .switchIfEmpty(Mono.defer(() -> loader.get()
            .flatMap(value -> cardRedis.opsForValue().set(key, value, cardByIdTtl)
                .thenReturn(value))));
  }

  public Mono<PrimaryAccountBalance> primaryBalance(String cardId, Supplier<Mono<PrimaryAccountBalance>> loader) {
    String key = PRIMARY_BALANCE + ":" + cardId;
    return pbRedis.opsForValue().get(key)
        .switchIfEmpty(Mono.defer(() -> loader.get()
            .flatMap(value -> pbRedis.opsForValue().set(key, value, primaryBalanceTtl)
                .thenReturn(value))));
  }

  public Flux<CardMovement> movements(String cardId, int limit, Supplier<Flux<CardMovement>> loader) {
    String key = MOVEMENTS + ":" + cardId + ":" + limit;
    return movRedis.opsForValue().get(key)
        .flatMapMany(Flux::fromArray)
        .switchIfEmpty(
            Mono.defer(() -> loader.get()
                    .collectList()
                    .flatMap(list -> movRedis.opsForValue()
                        .set(key, list.toArray(new CardMovement[0]), movementsTtl)
                        .thenReturn(list.toArray(new CardMovement[0]))))
                .flatMapMany(Flux::fromArray)
        );
  }
}
