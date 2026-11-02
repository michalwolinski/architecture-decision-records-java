package com.wolinski.adr;

import java.util.Map;

/** USD per million tokens: input, 5-minute cache write, cache read, output. */
record Pricing(double input, double cacheWrite, double cacheRead, double output) {

    private static final Map<String, Pricing> MODELS = Map.of(
            "claude-haiku-5-5", new Pricing(0.10, 0.125, 0.01, 0.50),
            "claude-haiku-4-5", new Pricing(1.00, 1.25, 0.10, 5.00),
            "claude-sonnet-5-5", new Pricing(2.00, 2.50, 0.20, 10.00),
            "claude-opus-5-5", new Pricing(4.00, 5.00, 0.20, 20.00));

    static Pricing of(String model) {
        Pricing pricing = MODELS.get(model);
        if (null == pricing) {
            throw new IllegalArgumentException("no price for " + model);
        }
        return pricing;
    }

    double cost(Agent.Usage usage) {
        return (usage.input() * input + usage.cacheWrite() * cacheWrite + usage.cacheRead() * cacheRead
                + usage.output() * output) / 1_000_000;
    }
}
