# 5. No remote calls inside a transaction

Date: 2026-05-11

## Status

Accepted

## Context

During a carrier outage, requests held their database connections for the full HTTP timeout, and the pool ran out for the whole shop.

## Decision

No call to a remote system runs while a transaction is active, directly or through a helper. The local change commits first, and the remote call happens after it.

## Consequences

The remote call can fail after the commit, so that outcome has to be handled explicitly. `@TransactionalEventListener` or an orchestrating method without a transaction are both acceptable.

`OrderReturns` predates this decision and is scheduled for migration. It is not a pattern to follow.
