# Architecture decision records in Java: write them, enforce them, measure them

By [Michał Woliński](https://wolinski.com). Companion code for the article
[Architecture Decision Records in Java](https://wolinski.com/blog/architecture-decision-records-java.html).

A small Spring shop with twelve architecture decision records, ArchUnit rules that enforce them, and a harness
that measures whether a coding agent follows them. A hidden grader judges the code the agent leaves behind,
never what it says. Every measurement in the article comes from a run committed under [`runs/`](runs). All paid
runs together cost $1.60, recomputed from the `usage` recorded in them.

## Run it

Docker and bash. The Maven and JDK 25 image is pinned by digest, and the repository is mounted read-only.

```sh
./run.sh test                                        # 73 unit tests + scripted agents, no API key, about 15 minutes
./run.sh eval --agent oracle                         # free: every ticket must pass in every arm
./run.sh eval --agent claude --reps 5 --budget-usd 2 # paid: Claude Haiku 5.5 by default
./run.sh report runs/<run-dir>
```

`--budget-usd` is a hard stop checked before each request. The key is passed to the container for one command
only; on macOS `run.sh` reads it from the keychain item `ANTHROPIC_API_KEY` when the variable is not set. The
code the model writes runs without the key in its environment, and as an unprivileged user. It still has network
access, so do not point this harness at a model or a task you do not trust.

## What is in it

```
fixture/base/               the shop the agent works on
fixture/overlays/folder/    docs/adr: the twelve records
fixture/overlays/enforced/  ArchitectureTest.java: the rules, with the record's number in `because`
fixture/agents/             the two versions of AGENTS.md
fixture/grader/             hidden: the verdict rules and one acceptance test per ticket
src/main/java/.../adr/
  Arm.java                  none, rules, folder, pointer, enforced
  Task.java                 the eleven tickets
  Workspace.java            one episode's copy of the shop, and the agent's only file system
  Grader.java               takes the agent's src/main only, and reports violations and tampering
  ClaudeAgent.java          a manual tool loop over the Messages API
  ScriptedAgents.java       oracle, shortcut, null, tamper and evader: the proof of the harness
  Main.java, Report.java    the runner, and the report recomputed from results.jsonl
src/test/                   the harness tests, including the scripted agents
runs/                       the recorded runs the article quotes
```

## The runs

| Run | What | Episodes | Cost | Notes |
| --- | --- | --- | --- | --- |
| `20261102T162807Z-…` | Single episode | 1 | $0.0019 | Checks the loop against the real API. First catalogue. |
| `20261102T162849Z-…` | Pilot 1 | 28 | $0.1241 | First catalogue. Saturated: the agent broke 1 of 6 tempted decisions with no records. Not a measurement. |
| `20261102T165128Z-…` | Pilot 2 | 28 | $0.1300 | Old code added that breaks each decision. Still 1 of 6. Not a measurement. |
| `20261102T171256Z-…` | Pilot 3 | 28 | $0.1369 | Team-specific decisions added. 4 of 6 with no records. Not a measurement. |
| `20261102T172051Z-…` | **Main run** | 220 | $1.0711 | 11 tickets x 4 arms x 5 repetitions. |
| `20261102T175700Z-…` | **Rules-only arm** | 30 | $0.1384 | 6 team tickets x 5 repetitions. |

The scripted runs (`oracle`, `shortcut`, `null`) cost nothing. All paid runs used `claude-haiku-5-5` at effort
`medium`.

**Read the raw reports with these corrections, which the article explains:**

- `pointer` / `cancel-mail` / 1 in the main run is a false alarm: a correct edit to `OrderReturns` cost that class
  its exemption. The article counts `pointer` as 0 of 20 on the common-practice tickets; the file says 1.
- The three `adr0003` hits on `discount` in the rules run are the same kind of false alarm, on `OrderPricing`.
- `any-viol` overstates: the ADR 11 rule also flags programming errors in record constructors. The headline counts
  only the decision a ticket tempts.
- The `last-payment` functional failures in the arms with records come from a conflict between that ticket and
  ADR 10, not from a model failure.
- `RefundNotifications` in the shop mails through a plain `@EventListener`, inside the transaction. That breaks ADR 5
  as written, and the runtime check only watches the carrier. It stays because the paid runs saw this shop.
- The repositories are in memory, so the grader cannot see the Spring trap of writing data after a commit without
  `REQUIRES_NEW`.
- The records are exactly what the paid runs saw, including ADR 2's rejected option, whose reason does not hold
  (Spring wires package-private classes without complaint). The shop and its history are invented.
- The shop, its records and the visible rules did not change after the main run started. What did: the rules arm in
  the harness, the report's interval formatting, the scripted reference solutions, one grader test (a missing method
  no longer counts as a violation of ADR 10), and a new test about Spring's transaction flag. The `report.txt` of the
  first two pilots predates the catalogue field, so `./run.sh report` shows only the per-ticket table for them.
- The harness does not store the final source tree, and long tool output is trimmed, so these runs cannot be
  re-graded.

## Claims and where they are proven

| Claim | Where |
| --- | --- |
| Each shortcut works and breaks exactly the decision it tempts | `ScriptedAgentsTest.eachShortcutWorksAndBreaksExactlyTheDecisionItTempts` |
| The visible rules catch every shortcut except ADR 7 and ADR 12 | `ScriptedAgentsTest.theEnforcedSuiteCatchesEveryShortcutExceptTheUnenforcedDecision` |
| Deleting the rules turns CI green and does not change the verdict | `ScriptedAgentsTest.deletingTheRulesTurnsTheSuiteGreenAndChangesNoVerdict` |
| An ignore file does the same, and is reported | `ScriptedAgentsTest.anIgnoreFileIsReportedAndDoesNotChangeTheVerdict` |
| A helper method gets past the visible ADR 5 rule, not the runtime check | `ScriptedAgentsTest.aHelperMethodGetsPastTheVisibleRuleButNotTheRuntimeCheck` |
| A local `double` is invisible to the rules, and found in the source | `ScriptedAgentsTest.aFloatingPointLocalIsFoundInTheSourceEvenThoughNoRuleSeesIt` |
| A class named as an exception hides new code from the visible rules | `ScriptedAgentsTest.addingToAClassThatPredatesTheDecisionCountsAndTheVisibleRuleMissesIt` |
| `isActualTransactionActive()` is still true in an after-commit callback | `SpringAfterCommitFlagTest`, run by `ScriptedAgentsTest.springStillReportsAnActiveTransactionAfterCommit` |
| The tools cannot leave the repository, and model code gets no API key | `WorkspaceTest` |
| A do-nothing agent breaks no decision on any ticket | `ScriptedAgentsTest.doingNothingBreaksNoDecisionOnAnyTicket` |

## Author

[Michał Woliński](https://wolinski.com). MIT license.
