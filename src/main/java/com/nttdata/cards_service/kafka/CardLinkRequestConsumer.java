package com.nttdata.cards_service.kafka;

import com.nttdata.cards_service.kafka.events.CardLinkRequestEvent;
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
    log.info("[CARDS] Solicitud de enlace recibida | phone={} | cardId={} | traceId={}", evt.getPhone(), evt.getDebitCardId(), evt.getTraceId());

    repo.findById(evt.getDebitCardId())
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Tarjeta no encontrada")))
        .flatMap(card -> {
          if (!"DEBIT".equalsIgnoreCase(card.getCardType())) {
              return Mono.error(new IllegalStateException("La tarjeta no es de tipo DEBIT"));
          }
            log.info("[CARDS] Validaciones OK | phone={} | cardId={} | tipo={}", evt.getPhone(), evt.getDebitCardId(), card.getCardType());
            return result.publishOk(evt.getPhone(), evt.getDebitCardId(), evt.getTraceId());
        })
        .doOnError(e -> {
            log.warn("[CARDS] Enlace RECHAZADO | phone={} | cardId={} | motivo={} | traceId={}", evt.getPhone(), evt.getDebitCardId(), e.getMessage(), evt.getTraceId());
          result.publishRejected(evt.getPhone(), evt.getDebitCardId(), evt.getTraceId(), e.getMessage())
                  .subscribe();
        })
        .subscribe();
  }
}