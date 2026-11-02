# 8. Rates are whole basis points

Date: 2026-08-10

## Status

Accepted

## Context

ADR 3 moved rates to `BigDecimal`. Since then we have had two bugs from it: `equals` treating `1.0` and `1.00` as different, and one `divide` without a rounding mode that threw in production.

## Decision

Supersedes ADR 3. Production code uses neither floating point nor `BigDecimal`. A rate is a whole number of basis points in a `long` (12.5% is 1250), and arithmetic on it is `long` arithmetic. Text input is converted to basis points by hand, and a division rounds half up by adding half of the divisor first.

## Consequences

A rate with more than two decimal places cannot be represented, and that is what we want. `OrderPricing` predates this decision and is scheduled for migration. It is not a pattern to follow.
