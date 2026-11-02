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

class LastPaymentAcceptanceTest {

    @Test
    void theFacadeReturnsTheLastPayment() throws Exception {
        try (var shop = ShopHarness.start()) {
            PaymentsFacade payments = shop.getBean(PaymentsFacade.class);
            payments.charge("o-1", Money.pln(100));
            payments.charge("o-1", Money.pln(250));
            var last = payments.lastPayment("o-1");
            assertEquals(Money.pln(250), last.amount());
            assertEquals("CAPTURED", last.status());
        }
    }
}
