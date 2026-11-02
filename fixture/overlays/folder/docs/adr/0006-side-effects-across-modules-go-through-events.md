# 6. Other modules cause notifications through events

Date: 2026-06-15

## Status

Accepted

## Context

Direct calls to `Notifier` from business modules made a slow mail server slow down orders, and each new notification changed a business module.

## Decision

Only the `notifications` module depends on `notifications`. A module that has something to announce publishes an event from its `api` package through Spring's `ApplicationEventPublisher`, and `notifications` listens to it.

## Consequences

`notifications` depends on the `api` of the modules it listens to, as `RefundNotifications` does.

`OrderReturns` predates this decision and is scheduled for migration. It is not a pattern to follow.
