package com.wolinski.shop.money;

public record Money(long minorUnits, String currency) {

    public static Money pln(long minorUnits) {
        return new Money(minorUnits, "PLN");
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(minorUnits + other.minorUnits, currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(minorUnits - other.minorUnits, currency);
    }

    private void requireSameCurrency(Money other) {
        if (false == currency.equals(other.currency)) {
            throw new IllegalArgumentException(currency + " vs " + other.currency);
        }
    }
}
