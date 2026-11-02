package com.wolinski.shop.payments.internal;

import com.wolinski.shop.money.Money;
import java.time.Instant;

public class Payment {

    private final String orderId;
    private final Money amount;
    private final Instant paidAt;
    private Money refunded;
    private String status = "CAPTURED";

    public Payment(String orderId, Money amount, Instant paidAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.paidAt = paidAt;
        this.refunded = new Money(0, amount.currency());
    }

    public String orderId() {
        return orderId;
    }

    public Money amount() {
        return amount;
    }

    public Instant paidAt() {
        return paidAt;
    }

    public Money refunded() {
        return refunded;
    }

    public String status() {
        return status;
    }

    public void addRefund(Money amount) {
        refunded = refunded.plus(amount);
        status = "REFUNDED";
    }
}
