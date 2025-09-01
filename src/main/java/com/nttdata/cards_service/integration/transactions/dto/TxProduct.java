package com.nttdata.cards_service.integration.transactions.dto;

public class TxProduct {
    private String id;
    private String type;
    private String number;

    public static TxProduct of(String id, String type) {
        TxProduct p = new TxProduct();
        p.id = id;
        p.type = type;
        return p;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}