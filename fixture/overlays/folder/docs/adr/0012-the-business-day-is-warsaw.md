# 12. The business day is in Europe/Warsaw

Date: 2026-10-05

## Status

Accepted

## Context

Accounting closes the day in Warsaw. An order placed at 23:30 Warsaw time in winter is 22:30 UTC, and the UTC-based numbers disagreed with accounting by every order placed around midnight.

## Decision

Any rule that depends on a calendar day uses the `Europe/Warsaw` time zone. Instants are still stored in UTC.

## Consequences

`OrderReports` predates this decision and is scheduled for migration. It is not a pattern to follow.
