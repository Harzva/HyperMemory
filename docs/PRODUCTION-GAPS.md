# Remaining Production Gaps

HyperMemory is the final product repository, but it is not finished as a production-grade memory system until the following gaps are closed.

## P0 Before Real Users

| Gap | Impact | Recommended fix |
| --- | --- | --- |
| No user authentication | Anyone reaching the API can upload, query, or run memory modes. | Add OAuth/OIDC or gateway auth before public exposure. |
| No tenant-scoped document isolation | Knowledge, memory, and Bot conversations can mix across organizations. | Add `tenantId` to documents, chunks, memory records, conversations, and retrieval filters. |
| HyperMemory is in memory | Restart loses important memory context. | Persist memory records in SQLite or MySQL with explicit lifecycle rules. |
| No Bot idempotency store | Platform retries can trigger duplicate memory/model calls. | Store `(channel, messageId)` in Redis or MySQL with TTL. |

## P1 Architectural Hardening

| Gap | Impact | Recommended fix |
| --- | --- | --- |
| HyperMemory lacks durable memory semantics | The final memory layer still behaves like an in-process aggregation demo. | Define explicit memory records, retention rules, and retrieval traces. |
| SQLite-only target is not implemented | Current stack is heavier than the desired final design. | Decide whether to migrate retrieval and memory to SQLite/SQLite-vss or keep Milvus. |
| No source citation response model | Operators cannot audit why an answer was produced. | Return answer plus memory layer, chunk IDs, document names, and scores. |
| `ddl-auto: update` | Schema changes are implicit. | Introduce Flyway/Liquibase migrations. |
| Base alert rules added | Prometheus alert rules exist for HTTP errors, QA errors, latency, memory, and traffic lulls. | Wire Prometheus/Alertmanager to the rule file and tune thresholds to real traffic. |

## P2 Product Refinement

| Gap | Impact | Recommended fix |
| --- | --- | --- |
| No memory evaluation set | Memory changes can silently regress. | Add golden multi-turn campus QA cases. |
| No retention policy | Long-term memory can grow without governance. | Add TTL, summarization, deletion, and export policies. |
| No admin console | Operators cannot inspect memory health. | Add an authenticated operator dashboard later. |

## Maintenance Focus

- Keep this repo as the final system, not another collection of versions.
- Remove duplicate memory abstractions when they stop adding real behavior.
- Update README screenshots whenever the workbench UI or mode list changes.
- Run `mvn -B test`, `npm run build`, and `npm audit --audit-level=moderate` before releases.
