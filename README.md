# cloud-itonami-isco-8211

Open Occupation Blueprint for **ISCO-08 8211**: Mechanical Machinery
Assemblers.

This repository designs a forkable OSS business for a mechanical
machinery assembly-line scheduling and logistics coordination
practice: a line scheduling and supply-coordination robot manages
crew/task records under a governor-gated actor, so a mechanical
machinery assembler crew keeps its own operating records instead of
renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/mechassemblycoord/` implements the
`MechAssemblyCoordActor` as a `langgraph.graph/state-graph`
(`mechassemblycoord.actor`) wired to a `Mechanical Assembly Line
Scheduling Coordination Advisor` (`mechassemblycoord.advisor`) and an
independent `MechAssemblyCoordGovernor` (`mechassemblycoord.governor`),
following the itonami actor pattern (ADR-2607121000): `:intake ->
:advise -> :govern -> :decide -+-> :commit (:ok? true) +->
:request-approval (:escalate? true, human-in-the-loop interrupt) +->
:hold (:hard? true)`. HARD invariants (always hold, never
overridable): assembler provenance, line provenance, no-actuation
(`:effect` must be `:propose`), a closed op-allowlist
(`:log-work-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would directly finalize an assembly-execution decision
(e.g. deciding to proceed with a specific assembly run) or a
line-safety-clearance decision (e.g. declaring an assembly line safety
cleared), or that would override a plant safety officer's judgment.
Always-escalate paths (human sign-off regardless of confidence,
mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a line scheduling/logistics coordination
robot performs crew scheduling, production-run/inventory/progress-record
logging and components/fasteners-stock supply-order coordination for a
mechanical machinery assembler crew, under an actor that proposes actions and
an independent **Mechanical Assembly Line Scheduling Coordination Governor**
that gates them. The governor never dispatches hardware itself, never
performs assembly work on the production line, and never finalizes an
assembly-execution decision or a line-safety-clearance decision, and never
overrides a plant safety officer's judgment; `:high`/`:safety-critical`
actions (such as a flagged pinch-point-hazard/tool-hazard/equipment-condition
concern, or an above-threshold supply order) require human sign-off. **This
actor coordinates LINE SCHEDULING/LOGISTICS ONLY — it never performs assembly
work itself, and it never makes a line-safety-clearance decision itself.**

Mechanical Machinery Assemblers assemble mechanical components on
production lines — a standard workshop hazard domain: hand-tool injury and
pinch-point/crush hazard from assembly fixtures and machinery. This is a
real physical worker-safety domain; this actor never performs that assembly
work and never clears a line as safe — it only schedules and logs around it,
and always routes safety concerns to a human plant safety officer.

## Core Contract

```text
crew roster + line registration + safety-reporting policy
        |
        v
Mechanical Assembly Line Scheduling Coordination Advisor -> MechAssemblyCoordGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
finalize an assembly-execution decision, finalize a line-safety-clearance
decision, override a plant safety officer's judgment, suppress an operating
record, or disclose sensitive data without governor approval and audit
evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8211`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
