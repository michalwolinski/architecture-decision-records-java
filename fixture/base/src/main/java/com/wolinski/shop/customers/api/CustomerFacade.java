package com.wolinski.shop.customers.api;

public interface CustomerFacade {

    void register(String customerId, String email);

    String emailOf(String customerId);
}
