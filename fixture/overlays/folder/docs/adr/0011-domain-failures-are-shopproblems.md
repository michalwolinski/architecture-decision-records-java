# 11. Domain failures are ShopProblems

Date: 2026-09-21

## Status

Accepted

## Context

The API layer turns exceptions into error responses. It could only map our failures by matching on messages.

## Decision

When a business rule refuses something, production code throws `platform.ShopProblem` with a stable code, or a subclass of it. It does not throw `IllegalStateException`, `IllegalArgumentException` or its own `RuntimeException` subclasses for that.

## Consequences

Codes become part of the API. `Money` and `OrderRepository` report programming errors, not business rules, and keep their exceptions. `OrderReturns` predates this decision and is scheduled for migration. It is not a pattern to follow.
