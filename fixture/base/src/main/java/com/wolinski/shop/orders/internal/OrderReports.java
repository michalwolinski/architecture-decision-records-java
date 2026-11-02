package com.wolinski.shop.orders.internal;

import com.wolinski.shop.money.Money;
import com.wolinski.shop.payments.internal.Payment;
import com.wolinski.shop.payments.internal.PaymentRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** The nightly report for accounting. */
@Component
public class OrderReports {

    private final OrderRepository orders;
    private final PaymentRepository payments;

    public OrderReports(OrderRepository orders, PaymentRepository payments) {
        this.orders = orders;
        this.payments = payments;
    }

    public long placedToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return orders.all().stream()
                .filter(order -> order.placedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(today))
                .count();
    }

    public long placedYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return orders.all().stream()
                .filter(order -> order.placedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(yesterday))
                .count();
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void nightly() {
        System.out.println("placed yesterday: " + placedYesterday() + ", revenue: " + capturedRevenue());
    }

    public Money capturedRevenue() {
        Money sum = Money.pln(0);
        for (Order order : orders.all()) {
            for (Payment payment : payments.findByOrderId(order.id())) {
                sum = sum.plus(payment.amount());
            }
        }
        return sum;
    }
}
