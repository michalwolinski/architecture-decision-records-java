package com.wolinski.adr;

/** Spend recorded from the API's usage blocks, and the hard stop on it. Shared by parallel episodes. */
public final class Ledger {

    private final double budgetUsd;
    private double spentUsd;
    private int consecutiveAuthFailures;

    public Ledger(double budgetUsd) {
        this.budgetUsd = budgetUsd;
    }

    public synchronized void record(double usd) {
        spentUsd += usd;
        consecutiveAuthFailures = 0;
    }

    public synchronized void authFailure() {
        consecutiveAuthFailures++;
    }

    public synchronized double spent() {
        return spentUsd;
    }

    public synchronized boolean exhausted() {
        return spentUsd >= budgetUsd;
    }

    public synchronized boolean authBroken() {
        return 3 <= consecutiveAuthFailures;
    }
}
