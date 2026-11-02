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

class OrderNoteAcceptanceTest {

    @Test
    void aNoteAppearsOnTheSummary() throws Exception {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            String id = orders.place("c-1", Money.pln(1), Instant.parse("2026-10-01T10:00:00Z"));
            assertEquals(null, orders.summary(id).note());
            orders.addNote(id, "leave at the door");
            assertEquals("leave at the door", orders.summary(id).note());
        }
    }
}
