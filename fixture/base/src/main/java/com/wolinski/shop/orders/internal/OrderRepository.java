package com.wolinski.shop.orders.internal;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public void save(Order order) {
        orders.put(order.id(), order);
    }

    public Order get(String orderId) {
        Order order = orders.get(orderId);
        if (null == order) {
            throw new NoSuchElementException(orderId);
        }
        return order;
    }

    public Collection<Order> all() {
        return orders.values();
    }
}
