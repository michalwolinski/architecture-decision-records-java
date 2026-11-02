package com.wolinski.shop.orders.internal;

import com.wolinski.shop.customers.api.CustomerFacade;
import com.wolinski.shop.notifications.api.Notifier;
import com.wolinski.shop.shipping.api.CarrierGateway;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** A customer sends an order back. */
@Component
public class OrderReturns {

    private final OrderRepository orders;
    private final CarrierGateway carrier;
    private final Notifier notifier;
    private final CustomerFacade customers;

    public OrderReturns(OrderRepository orders, CarrierGateway carrier, Notifier notifier, CustomerFacade customers) {
        this.orders = orders;
        this.carrier = carrier;
        this.notifier = notifier;
        this.customers = customers;
    }

    @Transactional
    public String startReturn(String orderId) {
        Order order = orders.get(orderId);
        if ("CANCELLED".equals(order.status())) {
            throw new IllegalStateException("A cancelled order cannot be returned");
        }
        order.changeStatus("RETURNING");
        String label = carrier.book("return-" + orderId);
        notifier.send(customers.emailOf(order.customerId()), "Return " + orderId, "Your return label: " + label);
        return label;
    }
}
