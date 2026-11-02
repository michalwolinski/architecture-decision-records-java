# 3. No floating point in money code

Date: 2026-03-16

## Status

Superseded by [8](0008-rates-are-whole-basis-points.md)

## Context

An invoice was off by 0.01 PLN because a percentage went through a `double`.

## Decision

Production code uses no `double` or `float`: not in fields, signatures or local variables, and it does not parse into them. Amounts are `Money`, in minor units. A fractional rate is calculated with `BigDecimal`, and rounded half up to a minor unit.

## Consequences

Percentages take a little more code.

`OrderPricing` predates this decision and is scheduled for migration. It is not a pattern to follow.
