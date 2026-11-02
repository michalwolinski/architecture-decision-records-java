package com.wolinski.shop.payments.api;

import com.wolinski.shop.money.Money;

public record PaymentRefunded(String orderId, Money amount) {
}
