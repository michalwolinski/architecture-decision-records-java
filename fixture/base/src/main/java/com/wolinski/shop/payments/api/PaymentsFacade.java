package com.wolinski.shop.payments.api;

import com.wolinski.shop.money.Money;
import com.wolinski.shop.payments.internal.Payment;
import java.util.List;

public interface PaymentsFacade {

    void charge(String orderId, Money amount);

    void refund(String orderId, Money amount);

    String status(String orderId);

    List<Payment> paymentsOf(String orderId);
}
