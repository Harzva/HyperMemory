# Maintenance Guide

## Release Checklist

1. Update screenshots if the UI changed.
2. Let GitHub Actions run backend validation with `mvn -B test`.
3. Let GitHub Actions run frontend validation with `npm ci` and `npm run build`.
4. Confirm the CI Docker image build and `docker compose config` jobs pass.
5. Review secrets: `.env`, API keys, Bot tokens, and uploaded documents must not be committed.
6. Tag the release after CI passes.

## Weekly Maintenance

| Area | Action |
| --- | --- |
| Dependencies | Review Dependabot PRs for Maven, npm, and GitHub Actions. |
| Security | Rotate shared test secrets and confirm `.env.example` contains placeholders only. |
| Operations | Check health, 5xx rate, p95 latency, JVM memory, and memory-mode failures. |
| Memory quality | Re-run golden multi-turn examples before changing memory prompts or storage. |
| Docs | Keep README, OpenAPI, Bot guide, and production gaps aligned with code. |
| Deployability | Keep Dockerfiles, Compose, and Kubernetes manifests passing CI validation. |

## Alert Suggestions

| Signal | Threshold | Action |
| --- | --- | --- |
| HTTP 5xx | More than 2% for 5 minutes | Inspect backend logs and provider errors. |
| p95 latency | More than 8 seconds for 10 minutes | Check model latency, retrieval latency, and memory aggregation. |
| Memory write failure | Any sustained failure | Inspect persistence layer and rollback recent memory changes. |
| Bot auth failures | Sudden spike | Check token rotation, replay attempts, or provider config. |

## Versioning

- Use semantic tags such as `v0.1.0`.
- Keep breaking API changes out of patch releases.
- Update `docs/openapi/bot-gateway.yaml` in the same PR as API behavior changes.
