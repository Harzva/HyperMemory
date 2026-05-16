# Production Readiness Status

HyperMemory is the final product repository. It now has the minimum closure needed for a controlled staging-to-production path.

## Closed Minimum Gates

| Gate | Status |
| --- | --- |
| API authentication and RBAC | Closed with token-based `USER`/`ADMIN` roles, tenant-bound access checks, and admin-only GBrain skill execution. Replace with OIDC before broad public use. |
| Tenant isolation | Closed for document upload, RAG retrieval, Wiki lookup, Agent/GBrain execution, HyperMemory records, and Bot dispatch through normalized `tenantId`. |
| Durable Wiki/GBrain/Hyper state | Closed through `wiki_pages`, `gbrain_skill_runs`, and `hyper_memory_records` JPA persistence. |
| Schema migration | Closed with Flyway baseline migration and `ddl-auto=validate`. |
| Golden QA in CI | Closed with offline golden suite validation in the CI workflow. |
| Bot idempotency and rate limit | Closed with Redis-backed duplicate suppression and fixed-window throttling. |
| Staging backup/restore and alert drill | Closed through `docs/STAGING-RUNBOOK.md` and `scripts/staging_drill.sh`. |

## Still Intentional Post-MVP Work

| Item | Why it remains |
| --- | --- |
| OAuth/OIDC login | Token RBAC is enough for a deployment gate, but not the final user identity system. |
| SQLite-only target | Current deployment remains MySQL/Milvus/MinIO; moving to SQLite/SQLite-vss is a separate product decision. |
| Memory retention governance | Bounded conversation retention exists; export, deletion, and summarization policy can follow. |
| Rich document parsing and reranking | Retrieval is stable enough for staging; quality work remains separate. |
| Operator console | Operations are documented through scripts and metrics; a dashboard can come later. |

## Maintenance Boundary

Keep this repo as the final system, not another collection of versions. Remove duplicate memory abstractions when they stop adding real behavior.
