package com.nttdata.cards_service.model.value;

import com.nttdata.cards_service.model.*;
import lombok.*;

import java.time.*;

@Data
public class StoredOperation {
  private String id;                        // operationId
  private String kind;                      // DEBIT_PAYMENT | DEBIT_WITHDRAWAL | PAY_CREDIT
  private OffsetDateTime createdAt;
  private CardOperationResponse result;     // DTO autogenerado
}
