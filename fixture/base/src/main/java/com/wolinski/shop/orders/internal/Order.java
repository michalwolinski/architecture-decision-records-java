package com.wolinski.shop.orders.internal;

import com.wolinski.shop.money.Money;
import java.time.Instant;

public class Order {

    private final String id;
    private final String customerId;
    private final Instant placedAt;
    private Money total;
    private String status = "PLACED";

    public Order(String id, String customerId, Money total, Instant placedAt) {
        this.id = id;
        this.customerId = customerId;
        this.total = total;
        this.placedAt = placedAt;
    }

    public String id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public Money total() {
        return total;
    }

    public void changeTotal(Money total) {
        this.total = total;
    }

    public String status() {
        return status;
    }

    public void changeStatus(String status) {
        this.status = status;
    }
}
