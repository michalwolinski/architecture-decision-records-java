package com.wolinski.adr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class LedgerAndReportTest {

    @Test
    void theBudgetIsAHardStop() {
        Ledger ledger = new Ledger(1.0);
        ledger.record(0.6);
        assertFalse(ledger.exhausted());
        ledger.record(0.4);
        assertTrue(ledger.exhausted());
    }

    @Test
    void threeAuthFailuresInARowStopTheRunAndASuccessResetsThem() {
        Ledger ledger = new Ledger(1.0);
        ledger.authFailure();
        ledger.authFailure();
        ledger.record(0.01);
        ledger.authFailure();
        ledger.authFailure();
        assertFalse(ledger.authBroken());
        ledger.authFailure();
        assertTrue(ledger.authBroken());
    }

    @Test
    void costCountsEachKindOfTokenAtItsOwnPrice() {
        double usd = Pricing.of("claude-sonnet-5-5").cost(new Agent.Usage(1_000_000, 1_000_000, 1_000_000, 1_000_000));
        assertEquals(2.00 + 2.50 + 0.20 + 10.00, usd, 1e-9);
    }

    @Test
    void wilsonMatchesAKnownInterval() {
        double[] interval = Report.wilson(8, 10);
        assertEquals(49.0, interval[0], 0.1);
        assertEquals(94.3, interval[1], 0.1);
    }

    @Test
    void onlyTheTargetDecisionCountsAsBreakingIt() throws Exception {
        ObjectMapper json = new ObjectMapper();
        assertTrue(Report.brokeTarget(json.readTree("{\"tempts\":\"adr0002\",\"violations\":[\"adr0002\"]}")));
        assertFalse(Report.brokeTarget(json.readTree("{\"tempts\":\"adr0002\",\"violations\":[\"adr0004\"]}")));
        assertFalse(Report.brokeTarget(json.readTree("{\"tempts\":null,\"violations\":[\"adr0004\"]}")));
    }
}
