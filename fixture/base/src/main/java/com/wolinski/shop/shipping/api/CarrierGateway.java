package com.wolinski.shop.shipping.api;

public interface CarrierGateway {

    /** Books a shipment with the carrier's HTTP API and returns its tracking number. */
    String book(String orderId);
}
