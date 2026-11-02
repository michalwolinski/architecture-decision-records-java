package com.wolinski.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wolinski.shop.money.Money;
import com.wolinski.shop.orders.api.OrderFacade;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NoCancelConfirmedAcceptanceTest {

    @Test
    void aConfirmedOrderStaysConfirmedAndAPlacedOneCanStillBeCancelled() {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            String confirmed = orders.place("c-1", Money.pln(1), Instant.parse("2026-10-01T10:00:00Z"));
            orders.confirm(confirmed);
            assertThrows(RuntimeException.class, () -> orders.cancel(confirmed));
            assertEquals("CONFIRMED", orders.summary(confirmed).status());
            String placed = orders.place("c-1", Money.pln(1), Instant.parse("2026-10-01T10:00:00Z"));
            orders.cancel(placed);
            assertEquals("CANCELLED", orders.summary(placed).status());
        }
    }
}
