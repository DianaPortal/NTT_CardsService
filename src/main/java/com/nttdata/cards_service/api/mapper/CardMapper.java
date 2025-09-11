package com.nttdata.cards_service.api.mapper;

import com.nttdata.cards_service.model.CardRequest;
import com.nttdata.cards_service.model.CardResponse;
import com.nttdata.cards_service.model.entity.Card;
import com.nttdata.cards_service.model.entity.enums.CardStatus;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.nttdata.cards_service.service.CardDomainUtils.normalizeAccounts;

@Component
@Slf4j
public class CardMapper {
    private static <T> T from(JsonNullable<T> v) {
        return v == null ? null : v.orElse(null);
    }
    private static <T> JsonNullable<T> toNullable(T v) {
        return v == null ? JsonNullable.undefined() : JsonNullable.of(v);
    }
    private static <T> T valueOf(JsonNullable<T> v) {
        return (v == null || !v.isPresent()) ? null : v.get();
    }


    public Card toEntity(CardRequest src) {
        log.debug("Mapeando CardRequest -> Card: {}", src);
        Card c = new Card();
        c.setId(src.getId());
        c.setCardNumber(src.getCardNumber());
        // entidad: cardType/brand = String; status = enum CardStatus
        c.setCardType(src.getCardType() == null ? null : src.getCardType().getValue());
        c.setBrand(src.getBrand() == null ? null : src.getBrand().getValue());
        c.setCustomerId(src.getCustomerId());
        c.setPrimaryAccountId(valueOf(src.getPrimaryAccountId()));
        c.setAccounts(src.getAccounts() == null ? new ArrayList<>() : src.getAccounts());
        c.setCreditId(valueOf(src.getCreditId()));
        c.setStatus(src.getStatus() == null ? null : CardStatus.valueOf(src.getStatus().getValue()));
        c.setIssueDate(src.getIssueDate());
        c.setExpirationDate(src.getExpirationDate());
        c.setIsVirtual(src.getIsVirtual());
        c.setPinEnabled(Boolean.TRUE.equals(src.getPinEnabled()));
        c.setContactlessEnabled(Boolean.TRUE.equals(src.getContactlessEnabled()));
        c.setLimits(src.getLimits());
        c.setMetadata(src.getMetadata());
        c.setCreationDate(src.getCreationDate());
        c.setUpdatedDate(src.getUpdatedDate());
        c.setAccounts(normalizeAccounts(c.getPrimaryAccountId(), c.getAccounts()));
        return c;
    }

    public CardResponse toResponse(Card src) {
        log.debug("Mapeando Card -> CardResponse: {}", src);
        CardResponse out = new CardResponse();
        out.setId(src.getId());
        out.setCardNumber(src.getCardNumber());
        out.setCardType(src.getCardType() == null ? null : CardResponse.CardTypeEnum.fromValue(src.getCardType()));
        out.setBrand(src.getBrand() == null ? null : CardResponse.BrandEnum.fromValue(src.getBrand()));
        out.setCustomerId(src.getCustomerId());
        out.setPrimaryAccountId(toNullable(src.getPrimaryAccountId()));
        out.setAccounts(src.getAccounts());
        out.setCreditId(toNullable(src.getCreditId()));
        out.setStatus(src.getStatus() == null ? null : CardResponse.StatusEnum.fromValue(src.getStatus().name()));
        out.setIssueDate(src.getIssueDate());
        out.setExpirationDate(src.getExpirationDate());
        out.setIsVirtual(src.getIsVirtual());
        out.setPinEnabled(src.isPinEnabled());
        out.setContactlessEnabled(src.isContactlessEnabled());
        out.setLimits(src.getLimits());
        out.setMetadata(src.getMetadata());
        out.setCreationDate(src.getCreationDate());
        out.setUpdatedDate(src.getUpdatedDate());
        return out;
    }

    public List<CardResponse> toResponseList(List<Card> cards) {
        return cards == null ? List.of() : cards.stream().filter(Objects::nonNull).map(this::toResponse).collect(Collectors.toList());
    }
}