package com.nttdata.cards_service.kafka.events;

import lombok.*;

import java.math.*;
import java.time.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrimaryBalanceUpdatedEvent { //Evaluar a considerar
  private String cardId;
  private String accountId;
  private BigDecimal balance;  // saldo de la cuenta principal tras la operación
  private String traceId;
  private Instant occurredAt;
}
