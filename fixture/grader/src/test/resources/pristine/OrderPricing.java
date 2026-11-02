package com.wolinski.shop.orders.internal;

import com.wolinski.shop.money.Money;
import org.springframework.stereotype.Component;

@Component
public class OrderPricing {

    private static final double VAT_PERCENT = 23.0;

    public Money withVat(Money net) {
        double gross = net.minorUnits() * (100 + VAT_PERCENT) / 100;
        return new Money(Math.round(gross), net.currency());
    }
}
