package com.wolinski.shop.shipping.internal;

import com.wolinski.shop.shipping.api.CarrierGateway;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpCarrierGateway implements CarrierGateway {

    private final RestClient carrier = RestClient.create("https://carrier.example/api");

    @Override
    public String book(String orderId) {
        return carrier.post().uri("/shipments/{orderId}", orderId).retrieve().body(String.class);
    }
}
