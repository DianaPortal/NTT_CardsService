package com.nttdata.cards_service.service;


import com.nttdata.cards_service.integration.accounts.*;
import com.nttdata.cards_service.model.*;
import com.nttdata.cards_service.repository.*;
import lombok.*;
import org.springframework.stereotype.*;
import reactor.core.publisher.*;

@Service
@RequiredArgsConstructor
public class PrimaryBalanceService {
  private final CardRepository repo;
  private final AccountsClient accounts;


  public Mono<PrimaryAccountBalance> get(String cardId) {
    return repo.findById(cardId)
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
            }
        );
  }
}
