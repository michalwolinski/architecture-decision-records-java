# 4. Time comes from the injected Clock

Date: 2026-04-07

## Status

Accepted

## Context

Time-dependent rules could only be tested by waiting, or by moving test data relative to the real date, and such tests failed around midnight.

## Decision

Production code reads the time only from the injected `java.time.Clock` bean. `Instant.now()`, `LocalDate.now()`, `System.currentTimeMillis()` and similar calls without a clock are not used.

## Consequences

Classes that need the time take a `Clock` constructor parameter.

`OrderReports` predates this decision and is scheduled for migration. It is not a pattern to follow.
