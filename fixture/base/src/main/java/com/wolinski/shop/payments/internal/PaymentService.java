package com.wolinski.shop.payments.internal;

import com.wolinski.shop.money.Money;
import com.wolinski.shop.payments.api.PaymentRefunded;
import com.wolinski.shop.payments.api.PaymentsFacade;
import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService implements PaymentsFacade {

    private final PaymentRepository payments;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public PaymentService(PaymentRepository payments, Clock clock, ApplicationEventPublisher events) {
        this.payments = payments;
        this.clock = clock;
        this.events = events;
    }

    @Override
    @Transactional
    public void charge(String orderId, Money amount) {
        payments.save(new Payment(orderId, amount, clock.instant()));
    }

    @Override
    @Transactional
    public void refund(String orderId, Money amount) {
        List<Payment> found = payments.findByOrderId(orderId);
        found.getLast().addRefund(amount);
        events.publishEvent(new PaymentRefunded(orderId, amount));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> paymentsOf(String orderId) {
        return payments.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public String status(String orderId) {
        return payments.findByOrderId(orderId).getLast().status();
    }
}
