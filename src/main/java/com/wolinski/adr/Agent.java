package com.wolinski.adr;

import java.util.List;
import java.util.Map;

/** Anything that can work on a ticket in a workspace: a model, or a scripted proof of the harness. */
public interface Agent {

    Outcome work(Task task, Workspace workspace, Ledger ledger) throws Exception;

    /** {@code status}: done, max_turns, truncated, refusal. */
    record Outcome(String status, String finalText, List<Map<String, Object>> trajectory, int turns, Usage usage,
            List<String> adrsRead, int testRuns) {
    }

    record Usage(long input, long cacheWrite, long cacheRead, long output) {

        static final Usage NONE = new Usage(0, 0, 0, 0);

        Usage plus(Usage other) {
            return new Usage(input + other.input, cacheWrite + other.cacheWrite, cacheRead + other.cacheRead,
                    output + other.output);
        }
    }
}
