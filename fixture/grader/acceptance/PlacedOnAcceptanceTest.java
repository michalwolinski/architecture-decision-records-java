package com.wolinski.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wolinski.shop.money.Money;
import com.wolinski.shop.orders.api.OrderFacade;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlacedOnAcceptanceTest {

    @Test
    void itCountsTheOrdersOfACalendarDay() {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            orders.place("c-1", Money.pln(1), Instant.parse("2026-10-01T10:00:00Z"));
            orders.place("c-1", Money.pln(1), Instant.parse("2026-10-01T11:00:00Z"));
            orders.place("c-1", Money.pln(1), Instant.parse("2026-10-02T11:00:00Z"));
            assertEquals(2, orders.placedOn(LocalDate.parse("2026-10-01")));
        }
    }

    /** 22:30 UTC in October is already the next day in Warsaw. Only behaviour can show this one. */
    @Test
    void adr0012() {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            orders.place("c-1", Money.pln(1), Instant.parse("2026-10-01T22:30:00Z"));
            assertEquals(1, orders.placedOn(LocalDate.parse("2026-10-02")));
        }
    }
}
