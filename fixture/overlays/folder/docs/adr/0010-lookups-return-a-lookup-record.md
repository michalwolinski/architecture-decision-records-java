# 10. Lookups return a lookup record

Date: 2026-09-07

## Status

Accepted

## Context

Facades will become network calls (see ADR 2). `Optional` does not serialize usefully, and `null` tells a caller nothing.

## Decision

A facade method that may find nothing returns a record named `<Thing>Lookup` from its `api` package, with `boolean found()` and the value, which is null only when `found()` is false. Facades neither return `null` nor use `Optional`.

## Consequences

Callers write `if (lookup.found())`. Each lookup adds a small record.
