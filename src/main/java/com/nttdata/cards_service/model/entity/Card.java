package com.nttdata.cards_service.model.entity;


import com.nttdata.cards_service.model.CardRequestLimits;
import com.nttdata.cards_service.model.entity.enums.CardStatus;
import com.nttdata.cards_service.model.value.StoredOperation;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data

@Document(collection = "cards")
public class Card {
    @Id
    @Indexed(unique = true, sparse = true)
    private String id;

    @Indexed(unique = true, sparse = true)
    private String cardNumber;             //# de tarjeta
    private String cardType;              // DEBIT | CREDIT
    private String brand;                // VISA | MASTERCARD | AMEX | OTHER
    private String customerId;          //Id Cliente

    //DEBIT
    private String primaryAccountId;    //Cuenta principal
    private List<String> accounts = new ArrayList<>();      //Cuentas adicionales
    //CREDIT
    private String creditId;

    private CardStatus status = CardStatus.ACTIVE;
    private LocalDate issueDate;        //Fecha de emisión de la tarjeta
    private LocalDate expirationDate;   //Fecha de vencimiento de la tarjeta

    private Boolean isVirtual;
    private boolean pinEnabled = true;
    private boolean contactlessEnabled = true;

    private CardRequestLimits limits;

    private Map<String, Object> metadata = new HashMap<>();

    private OffsetDateTime creationDate;
    private OffsetDateTime updatedDate;

    private List<StoredOperation> operations = new ArrayList<>();

    @Version
    private Long version;

}
