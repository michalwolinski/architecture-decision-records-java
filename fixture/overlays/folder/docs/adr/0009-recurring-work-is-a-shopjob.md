# 9. Recurring work is a ShopJob

Date: 2026-08-24

## Status

Accepted

## Context

We run several instances, and `@Scheduled` fires on each of them. Our scheduler is external, and it reads the jobs from the application at startup.

## Decision

Recurring work is a Spring bean implementing `platform.ShopJob`. We do not use `@Scheduled`, `TaskScheduler` or `ScheduledExecutorService`.

## Consequences

A job is visible to operations by name. `OrderReports.nightly` predates this decision and is scheduled for migration. It is not a pattern to follow.
