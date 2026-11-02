package com.wolinski.shop;

import com.wolinski.shop.notifications.api.MailTransport;
import com.wolinski.shop.platform.InMemoryTransactionManager;
import com.wolinski.shop.shipping.api.CarrierGateway;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** The shop with its two remote systems replaced by recorders. */
final class ShopHarness {

    static final List<String> MAIL = new CopyOnWriteArrayList<>();
    static final List<Boolean> CARRIER_CALLED_IN_TRANSACTION = new CopyOnWriteArrayList<>();

    private ShopHarness() {
    }

    static AnnotationConfigApplicationContext start() {
        MAIL.clear();
        CARRIER_CALLED_IN_TRANSACTION.clear();
        return new AnnotationConfigApplicationContext(ShopConfig.class, Fakes.class);
    }

    @Configuration
    static class Fakes {

        @Bean
        @Primary
        MailTransport recordingMail() {
            return (address, subject, body) -> MAIL.add(address + "|" + subject);
        }

        @Bean
        @Primary
        CarrierGateway recordingCarrier() {
            return orderId -> {
                CARRIER_CALLED_IN_TRANSACTION.add(InMemoryTransactionManager.isOpen());
                return "TRACK-" + orderId;
            };
        }
    }

    static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; 40 > i && false == condition.getAsBoolean(); i++) {
            Thread.sleep(50);
        }
    }
}
