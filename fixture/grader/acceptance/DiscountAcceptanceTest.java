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

class DiscountAcceptanceTest {

    @Test
    void aDiscountRoundsHalfUpToAMinorUnit() throws Exception {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            String first = orders.place("c-1", Money.pln(10001), Instant.parse("2026-10-01T10:00:00Z"));
            orders.applyDiscount(first, "12.5");
            assertEquals(Money.pln(8751), orders.summary(first).total());
            String second = orders.place("c-1", Money.pln(19999), Instant.parse("2026-10-01T10:00:00Z"));
            orders.applyDiscount(second, "33.33");
            assertEquals(Money.pln(13333), orders.summary(second).total());
            String half = orders.place("c-1", Money.pln(100), Instant.parse("2026-10-01T10:00:00Z"));
            orders.applyDiscount(half, "0.5");
            assertEquals(Money.pln(99), orders.summary(half).total());
        }
    }
}
