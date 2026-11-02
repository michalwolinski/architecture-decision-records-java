package com.wolinski.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wolinski.shop.money.Money;
import com.wolinski.shop.orders.api.OrderFacade;
import com.wolinski.shop.platform.ShopJob;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.scheduling.annotation.Scheduled;

class NightlyExpiryAcceptanceTest {

    @Test
    void staleOrdersAreCancelled() throws Exception {
        try (var shop = ShopHarness.start()) {
            OrderFacade orders = shop.getBean(OrderFacade.class);
            Instant now = shop.getBean(Clock.class).instant();
            String stale = orders.place("c-1", Money.pln(1), now.minus(Duration.ofDays(31)));
            String fresh = orders.place("c-1", Money.pln(1), now.minus(Duration.ofDays(29)));
            assertEquals(1, orders.expireStale());
            assertEquals("CANCELLED", orders.summary(stale).status());
            assertEquals("PLACED", orders.summary(fresh).status());
        }
    }

    /** Either mechanism counts as "runs on that schedule" here; which one is allowed is the decision's business. */
    @Test
    void somethingIsScheduledForTwoOClock() {
        try (var shop = ShopHarness.start()) {
            boolean scheduled = shop.getBeansOfType(ShopJob.class).values().stream()
                    .anyMatch(job -> job.cron().contains("2"));
            for (String name : shop.getBeanDefinitionNames()) {
                for (Method method : AopUtils.getTargetClass(shop.getBean(name)).getDeclaredMethods()) {
                    Scheduled annotation = method.getAnnotation(Scheduled.class);
                    scheduled |= null != annotation && annotation.cron().contains("2");
                }
            }
            assertTrue(scheduled);
        }
    }
}
