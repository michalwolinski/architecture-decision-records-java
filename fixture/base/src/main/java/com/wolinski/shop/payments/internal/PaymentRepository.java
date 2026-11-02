package com.wolinski.shop.payments.internal;

import com.wolinski.shop.money.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {

    private final Map<String, List<Payment>> payments = new ConcurrentHashMap<>();

    public void save(Payment payment) {
        payments.computeIfAbsent(payment.orderId(), id -> new ArrayList<>()).add(payment);
    }

    public List<Payment> findByOrderId(String orderId) {
        return payments.getOrDefault(orderId, List.of());
    }

    public Money refundedFor(String orderId) {
        return findByOrderId(orderId).stream()
                .map(Payment::refunded)
                .reduce(Money::plus)
                .orElse(Money.pln(0));
    }
}
