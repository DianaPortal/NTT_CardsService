package com.nttdata.cards_service.service;

import com.nttdata.cards_service.cache.CardsCacheService;
import com.nttdata.cards_service.integration.accounts.AccountsClient;
import com.nttdata.cards_service.model.PrimaryAccountBalance;
import com.nttdata.cards_service.repository.CardRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrimaryBalanceService {
    private final CardRepository repo;
    private final AccountsClient accounts;
    private final CardsCacheService cache;



    public Mono<PrimaryAccountBalance> get(String cardId) {
        return cache.primaryBalance(cardId, () ->
            repo.findById(cardId)
                .flatMap(card -> {
                if (card.getPrimaryAccountId() == null) return Mono.empty();
                return accounts.getAccount(card.getPrimaryAccountId())
                    .map(acc -> {
                        PrimaryAccountBalance out = new PrimaryAccountBalance();
                        out.setCardId(card.getId());
                        out.setAccountId(acc.getId());
                        out.setBalance(acc.getBalance());
                        return out;
                    });
            })
        );
    }
}
