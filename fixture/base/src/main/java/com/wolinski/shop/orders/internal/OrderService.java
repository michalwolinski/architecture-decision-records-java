package com.wolinski.shop.orders.internal;

import com.wolinski.shop.money.Money;
import com.wolinski.shop.orders.api.OrderFacade;
import com.wolinski.shop.orders.api.OrderSummary;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService implements OrderFacade {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    @Transactional
    public String place(String customerId, Money total, Instant placedAt) {
        Order order = new Order(UUID.randomUUID().toString(), customerId, total, placedAt);
        orders.save(order);
        return order.id();
    }

    @Override
    @Transactional
    public void confirm(String orderId) {
        orders.get(orderId).changeStatus("CONFIRMED");
    }

    @Override
    @Transactional
    public void cancel(String orderId) {
        orders.get(orderId).changeStatus("CANCELLED");
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSummary summary(String orderId) {
        Order order = orders.get(orderId);
        return new OrderSummary(order.id(), order.customerId(), order.total(), order.status());
    }
}
