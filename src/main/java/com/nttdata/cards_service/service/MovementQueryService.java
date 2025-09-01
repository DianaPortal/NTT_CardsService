package com.nttdata.cards_service.service;

import com.nttdata.cards_service.integration.transactions.TransactionsClient;
import com.nttdata.cards_service.integration.transactions.dto.TxGet;
import com.nttdata.cards_service.model.CardMovement;
import com.nttdata.cards_service.model.CardMovementProduct;
import com.nttdata.cards_service.repository.CardRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

import static com.nttdata.cards_service.service.CardDomainUtils.normalizeAccounts;

@Service
public class MovementQueryService {

    private final CardRepository repo;
    private final TransactionsClient tx;

    public MovementQueryService(CardRepository repo, TransactionsClient tx) {
        this.repo = repo; this.tx = tx;
    }

    public Flux<CardMovement> lastMovements(String cardId, int limit) {
        return repo.findById(cardId).flatMapMany(card -> {
            if ("DEBIT".equals(card.getCardType())) {
                List<String> products = normalizeAccounts(card.getPrimaryAccountId(), card.getAccounts());
                return Flux.fromIterable(products)
                        .flatMap(tx::findByProduct)
                        .sort(Comparator.comparing(TxGet::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .take(limit)
                        .map(t -> mapMovementFromSender(t));
            } else {
                return tx.findByProduct(card.getCreditId())
                        .sort(Comparator.comparing(TxGet::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .take(limit)
                        .map(t -> mapMovementFromReceiver(t));
            }
        });
    }

    private CardMovement mapMovementFromSender(TxGet t) {
        CardMovement cm = new CardMovement();
        cm.setId(t.getId());
        cm.setCreatedDate(t.getCreatedDate());
        cm.setAmount(t.getAmount());
        cm.setType(CardMovement.TypeEnum.fromValue(t.getType())); // enum
        CardMovementProduct p = new CardMovementProduct();
        if (t.getSender() != null) {
            p.setId(t.getSender().getId());
            p.setType(CardMovementProduct.TypeEnum.fromValue(t.getSender().getType()));
            p.setNumber(JsonNullable.of(t.getSender().getNumber())); // JsonNullable
        }
        cm.setProduct(p);
        return cm;
    }

    private CardMovement mapMovementFromReceiver(TxGet t) {
        CardMovement cm = new CardMovement();
        cm.setId(t.getId());
        cm.setCreatedDate(t.getCreatedDate());
        cm.setAmount(t.getAmount());
        cm.setType(CardMovement.TypeEnum.fromValue(t.getType()));
        CardMovementProduct p = new CardMovementProduct();
        if (t.getReceiver() != null) {
            p.setId(t.getReceiver().getId());
            p.setType(CardMovementProduct.TypeEnum.fromValue(t.getReceiver().getType()));
            p.setNumber(JsonNullable.of(t.getReceiver().getNumber()));
        }
        cm.setProduct(p);
        return cm;
    }
}