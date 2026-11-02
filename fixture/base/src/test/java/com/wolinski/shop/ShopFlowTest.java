package com.wolinski.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wolinski.shop.money.Money;
import com.wolinski.shop.orders.api.OrderFacade;
import com.wolinski.shop.payments.api.PaymentsFacade;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ShopFlowTest {

    @Test
    void anOrderCanBePlacedPaidAndCancelled() {
        try (var shop = new AnnotationConfigApplicationContext(ShopConfig.class)) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            PaymentsFacade payments = shop.getBean(PaymentsFacade.class);

            String id = orders.place("c-1", Money.pln(1000), Instant.parse("2026-10-01T10:00:00Z"));
            payments.charge(id, Money.pln(1000));
            orders.cancel(id);

            assertEquals("CANCELLED", orders.summary(id).status());
            assertEquals("CAPTURED", payments.status(id));
        }
    }
}
