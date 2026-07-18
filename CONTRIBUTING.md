# Contributing

`cloud-itonami-isco-8211` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
clojure -M:test
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real assembler, line or operator data, credentials or
  operating documents.
- Keep production writes and disclosures behind Mechanical Assembly Line Scheduling Coordination Governor.
- Treat this occupation's workflows as high-risk: add tests for permission,
  scope-exclusion, safety-escalation and audit logging.
- Never widen the closed op-allowlist to include an assembly-execution op, a
  line-safety-clearance op, or a plant-safety-officer-override op, without a
  dedicated ADR and explicit human review.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
