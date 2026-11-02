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

class RefundOnSummaryAcceptanceTest {

    @Test
    void theSummaryShowsWhatWasRefunded() throws Exception {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            PaymentsFacade payments = shop.getBean(PaymentsFacade.class);
            String id = orders.place("c-1", Money.pln(5000), Instant.parse("2026-10-01T10:00:00Z"));
            assertEquals(Money.pln(0), orders.summary(id).refunded());
            payments.charge(id, Money.pln(5000));
            payments.refund(id, Money.pln(1200));
            assertEquals(Money.pln(1200), orders.summary(id).refunded());
        }
    }
}
