package com.wolinski.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.wolinski.shop.money.Money;
import com.wolinski.shop.orders.api.OrderFacade;
import com.wolinski.shop.orders.api.OrderSummary;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** By reflection, because the return type is exactly what the decision is about. */
class LatestForCustomerAcceptanceTest {

    private static boolean exists() {
        return Arrays.stream(OrderFacade.class.getMethods()).anyMatch(method -> "latestForCustomer".equals(method.getName()));
    }

    private static Object call(OrderFacade orders, String customerId) throws Exception {
        for (Method method : OrderFacade.class.getMethods()) {
            if ("latestForCustomer".equals(method.getName())) {
                return method.invoke(orders, customerId);
            }
        }
        throw new AssertionError("no latestForCustomer on OrderFacade");
    }

    private static OrderSummary valueOf(Object result) throws Exception {
        if (result instanceof OrderSummary summary) {
            return summary;
        }
        if (result instanceof Optional<?> optional) {
            return (OrderSummary) optional.orElse(null);
        }
        if (null == result) {
            return null;
        }
        for (Method accessor : result.getClass().getMethods()) {
            if (OrderSummary.class == accessor.getReturnType() && 0 == accessor.getParameterCount()) {
                accessor.setAccessible(true);
                return (OrderSummary) accessor.invoke(result);
            }
        }
        throw new AssertionError("cannot find the summary in " + result);
    }

    @Test
    void itGivesTheLatestOrderAndTellsWhenThereIsNone() throws Exception {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            orders.place("c-1", Money.pln(1), Instant.parse("2026-10-01T10:00:00Z"));
            String latest = orders.place("c-1", Money.pln(2), Instant.parse("2026-10-02T10:00:00Z"));
            assertEquals(latest, valueOf(call(orders, "c-1")).id());
            assertEquals(null, valueOf(call(orders, "c-without-orders")));
        }
    }

    /** ADR-10 also forbids returning null, which no structural rule can see. A missing feature breaks no decision. */
    @Test
    void adr0010() throws Exception {
        if (false == exists()) {
            return;
        }
        try (var shop = ShopHarness.start()) {
            Object nothing = call(shop.getBean(OrderFacade.class), "c-without-orders");
            assertNotEquals(null, nothing);
            assertFalse(nothing instanceof Optional);
        }
    }
}
