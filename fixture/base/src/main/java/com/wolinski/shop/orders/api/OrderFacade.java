package com.wolinski.shop.orders.api;

import com.wolinski.shop.money.Money;
import java.time.Instant;

public interface OrderFacade {

    String place(String customerId, Money total, Instant placedAt);

    void confirm(String orderId);

    void cancel(String orderId);

    OrderSummary summary(String orderId);
}
