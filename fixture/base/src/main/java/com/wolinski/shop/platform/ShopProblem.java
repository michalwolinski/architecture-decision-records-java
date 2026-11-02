package com.wolinski.shop.platform;

/** A domain failure the API layer can show to a person: a stable code, and a message. */
public class ShopProblem extends RuntimeException {

    private final String code;

    public ShopProblem(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
