package com.nttdata.cards_service.kafka;

import com.nttdata.cards_service.kafka.dto.*;
import com.nttdata.cards_service.repository.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.kafka.annotation.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardLinkRequestConsumer {

  private final CardRepository repo;
  private final CardLinkResultProducer result;

  @KafkaListener(topics = "${app.topics.card-link-request}", groupId = "${spring.kafka.consumer.group-id}")
  public void onLinkRequest(CardLinkRequestEvent evt) {
    log.info("[CARDS] link.request phone={} cardId={} traceId={}", evt.getPhone(), evt.getDebitCardId(), evt.getTraceId());

    repo.findById(evt.getDebitCardId())
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Card not found")))
        .flatMap(card -> {
          if (!"DEBIT".equalsIgnoreCase(card.getCardType()))
            return Mono.error(new IllegalStateException("Not a DEBIT card"));
          // Si manejas estado, valida ACTIVE aquí (ej: card.getStatus())
          return result.publishOk(evt.getPhone(), evt.getDebitCardId(), evt.getTraceId());
        })
        .doOnError(e -> {
          log.warn("[CARDS] link.request REJECTED: {}", e.getMessage());
          result.publishRejected(evt.getPhone(), evt.getDebitCardId(), evt.getTraceId(), e.getMessage()).subscribe();
        })
        .subscribe();
  }
}