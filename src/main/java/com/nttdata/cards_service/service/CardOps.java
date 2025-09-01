package com.nttdata.cards_service.service;

import com.nttdata.cards_service.model.entity.Card;
import com.nttdata.cards_service.model.value.StoredOperation;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CardOps {
    private CardOps(){}
    public static Optional<StoredOperation> findOperation(Card card, String opId) {
        var ops = card.getOperations();
        if (ops == null) return Optional.empty();
        return ops.stream().filter(o -> opId.equals(o.getId())).findFirst();
    }

    public static void upsertOperation(Card card, StoredOperation op, int keepLastN) {
        var ops = card.getOperations();
        if (ops == null) ops = new ArrayList<>();
        ops = ops.stream()
                .filter(prev -> !op.getId().equals(prev.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        ops.add(0, op);
        if (ops.size() > keepLastN) { ops = ops.stream().limit(keepLastN).collect(Collectors.toList()); }
        card.setOperations(ops);
    }
}
