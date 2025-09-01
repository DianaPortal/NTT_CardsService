package com.nttdata.cards_service.repository;

import com.nttdata.cards_service.model.entity.Card;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface CardRepository extends ReactiveMongoRepository<Card, String> {
    Flux<Card> findByCustomerId(String customerId);
}
