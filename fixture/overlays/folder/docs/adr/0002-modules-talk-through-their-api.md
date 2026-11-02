# 2. Modules talk through their api package

Date: 2026-03-09

## Status

Accepted

## Context

We plan to split `payments` into its own service. Each direct use of another module's repository or entity would become a rewrite on that day.

## Decision

A class may use another module only through that module's `api` package. Classes in another module's `internal` package are off limits, even when they are public. If the API lacks something, add it to the API.

## Consequences

Some API methods exist only for one caller. We accept that: they are the list of what a future network call has to carry.
`OrderReports` predates this decision and uses `PaymentRepository`. It is scheduled for migration, and is not a pattern to follow.

## Options rejected

- Package-private internals: blocked by Spring's wiring across our current packages.
