package com.wolinski.shop.orders.api;

import com.wolinski.shop.money.Money;

public record OrderSummary(String id, String customerId, Money total, String status) {
}
