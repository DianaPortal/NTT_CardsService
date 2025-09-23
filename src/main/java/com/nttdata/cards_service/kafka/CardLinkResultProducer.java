package com.nttdata.cards_service.kafka;


import com.nttdata.cards_service.kafka.events.CardLinkResultEvent;

import com.nttdata.cards_service.kafka.dto.*;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

import java.time.*;

@Service
@RequiredArgsConstructor
public class CardLinkResultProducer {

  private final KafkaTemplate<String, Object> kafka;

  @Value("${app.topics.card-link-result}")

  private String topicCardLinkResult;

  public Mono<Void> publishOk(String phone, String cardId, String traceId) {
    var evt = new CardLinkResultEvent(phone, cardId, "OK", null, traceId, Instant.now());
    return Mono.fromFuture(kafka.send(topicCardLinkResult, phone, evt).completable()).then();

  private String topic;

  public Mono<Void> publishOk(String phone, String cardId, String traceId) {
    var evt = new CardLinkResultEvent(phone, cardId, "OK", null, traceId, Instant.now());
    return Mono.fromFuture(kafka.send(topic, phone, evt).completable()).then();

  }

  public Mono<Void> publishRejected(String phone, String cardId, String traceId, String reason) {
    var evt = new CardLinkResultEvent(phone, cardId, "REJECTED", reason, traceId, Instant.now());

    return Mono.fromFuture(kafka.send(topicCardLinkResult, phone, evt).completable()).then();

    return Mono.fromFuture(kafka.send(topic, phone, evt).completable()).then();

  }
}