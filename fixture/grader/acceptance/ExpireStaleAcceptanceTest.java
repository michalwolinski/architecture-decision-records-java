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

class ExpireStaleAcceptanceTest {

    @Test
    void placedOrdersOlderThanThirtyDaysAreCancelled() throws Exception {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            java.time.Clock clock = shop.getBean(java.time.Clock.class);
            Instant now = clock.instant();
            String stale = orders.place("c-1", Money.pln(1), now.minus(Duration.ofDays(31)));
            String fresh = orders.place("c-1", Money.pln(1), now.minus(Duration.ofDays(29)));
            String confirmed = orders.place("c-1", Money.pln(1), now.minus(Duration.ofDays(31)));
            orders.confirm(confirmed);
            assertEquals(1, orders.expireStale());
            assertEquals("CANCELLED", orders.summary(stale).status());
            assertEquals("PLACED", orders.summary(fresh).status());
            assertEquals("CONFIRMED", orders.summary(confirmed).status());
        }
    }
}
