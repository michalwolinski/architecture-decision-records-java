package com.wolinski.adr;

/** What the repository tells the agent about its architecture decisions. Everything else is identical. */
public enum Arm {
    /** The decisions exist nowhere. */
    NONE("none", "AGENTS.base.md"),
    /** No records and no pointer: only the ArchUnit rules, and what their failure messages say. */
    RULES("rules", "AGENTS.base.md"),
    /** docs/adr exists, and nothing points at it: the forgotten folder. */
    FOLDER("folder", "AGENTS.base.md"),
    /** docs/adr exists, and the instructions the agent gets say to read and follow it. */
    POINTER("pointer", "AGENTS.pointer.md"),
    /** As POINTER, plus ArchUnit rules in the test suite the agent runs. */
    ENFORCED("enforced", "AGENTS.pointer.md");

    final String label;
    final String agentsFile;

    Arm(String label, String agentsFile) {
        this.label = label;
        this.agentsFile = agentsFile;
    }

    boolean hasDecisions() {
        return this != NONE && this != RULES;
    }

    boolean hasRules() {
        return this == ENFORCED || this == RULES;
    }

    static Arm of(String label) {
        for (Arm arm : values()) {
            if (arm.label.equals(label)) {
                return arm;
            }
        }
        throw new IllegalArgumentException(label);
    }
}
