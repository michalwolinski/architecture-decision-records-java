package com.wolinski.shop.notifications.internal;

import com.wolinski.shop.notifications.api.Notifier;
import com.wolinski.shop.payments.api.PaymentRefunded;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RefundNotifications {

    private final Notifier notifier;

    public RefundNotifications(Notifier notifier) {
        this.notifier = notifier;
    }

    @EventListener
    void on(PaymentRefunded refunded) {
        notifier.send("accounting@shop.example", "Refund " + refunded.orderId(), refunded.amount().toString());
    }
}
