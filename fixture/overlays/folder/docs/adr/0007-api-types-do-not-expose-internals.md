# 7. API types do not expose internals

Date: 2026-07-20

## Status

Accepted

## Context

Returning an entity from a facade lets every caller depend on its shape, so renaming a field in `internal` breaks other modules.

## Decision

Nothing in an `api` package refers to a type in an `internal` package. A facade returns a record declared in its own `api` package.

## Consequences

There is a small mapping step between entities and API records.

`PaymentsFacade.paymentsOf` predates this decision and is scheduled for migration. It is not a pattern to follow.
