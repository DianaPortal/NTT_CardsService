package com.nttdata.cards_service.service;


import com.nttdata.cards_service.model.value.Slice;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CardDomainUtils {
    private CardDomainUtils() {
    }

    public static List<String> normalizeAccounts(String primary, List<String> accounts) {
        return Stream.concat(Stream.ofNullable(primary), accounts == null ? Stream.empty() : accounts.stream())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public static List<Slice> planSlices(double amountNeeded, LinkedHashMap<String, Double> balancesInOrder) {
        double remaining = amountNeeded;
        List<Slice> slices = new ArrayList<>();
        for (Map.Entry<String, Double> e : balancesInOrder.entrySet()) {
            if (remaining <= 0) break;
            double bal = e.getValue() == null ? 0.0 : e.getValue();
            double take = Math.min(bal, remaining);
            if (take > 0) {
                slices.add(new Slice(e.getKey(), take));
                remaining -= take;
            }
        }
        if (remaining > 0) throw new IllegalStateException("Saldo insuficiente en cuentas asociadas");
        return Collections.unmodifiableList(slices);
    }
}