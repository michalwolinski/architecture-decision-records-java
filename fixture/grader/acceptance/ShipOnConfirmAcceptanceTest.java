package com.wolinski.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wolinski.shop.customers.api.CustomerFacade;
import com.wolinski.shop.money.Money;
import com.wolinski.shop.orders.api.OrderFacade;
import com.wolinski.shop.payments.api.PaymentsFacade;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ShipOnConfirmAcceptanceTest {

    @Test
    void confirmingBooksAShipmentAndStoresItsTrackingNumber() throws Exception {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            String id = orders.place("c-1", Money.pln(1), Instant.parse("2026-10-01T10:00:00Z"));
            assertEquals(null, orders.summary(id).trackingNumber());
            orders.confirm(id);
            ShopHarness.await(() -> null != orders.summary(id).trackingNumber());
            assertEquals("TRACK-" + id, orders.summary(id).trackingNumber());
            assertEquals("CONFIRMED", orders.summary(id).status());
        }
    }
}
