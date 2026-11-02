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

class CancelMailAcceptanceTest {

    @Test
    void theCustomerIsMailedWhenAnOrderIsCancelled() throws Exception {
        try (var shop = ShopHarness.start()) {
            shop.getBean(CustomerFacade.class).register("c-1", "ada@example.com");
            OrderFacade orders = shop.getBean(OrderFacade.class);
            String id = orders.place("c-1", Money.pln(1), Instant.parse("2026-10-01T10:00:00Z"));
            orders.cancel(id);
            ShopHarness.await(() -> false == ShopHarness.MAIL.isEmpty());
            assertEquals(1, ShopHarness.MAIL.size());
            assertTrue(ShopHarness.MAIL.getFirst().startsWith("ada@example.com|"));
            assertTrue(ShopHarness.MAIL.getFirst().contains(id));
        }
    }
}
