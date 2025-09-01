package com.nttdata.cards_service.service;

import com.nttdata.cards_service.integration.accounts.AccountsClient;
import com.nttdata.cards_service.model.PrimaryAccountBalance;
import com.nttdata.cards_service.repository.CardRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PrimaryBalanceService {
    private final CardRepository repo;
    private final AccountsClient accounts;

    public PrimaryBalanceService(CardRepository repo, AccountsClient accounts) {
        this.repo = repo;
        this.accounts = accounts;
    }

    public Mono<PrimaryAccountBalance> get(String cardId) {
        return repo.findById(cardId).flatMap(card -> {
            if (card.getPrimaryAccountId() == null) return Mono.empty();
            return accounts.getAccount(card.getPrimaryAccountId())
                    .map(acc -> {
                        PrimaryAccountBalance out = new PrimaryAccountBalance();
                        out.setCardId(card.getId());
                        out.setAccountId(acc.getId());
                        out.setBalance(acc.getBalance());
                        return out;
                    });
        });
    }
}
