# Operations Guide

## Local Runtime

```bash
cp .env.example .env
docker compose up -d --build
```

Open:

- Frontend: `http://localhost:3000`
- Backend health: `http://localhost:8080/actuator/health`
- Backend metrics: `http://localhost:8080/actuator/prometheus`
- MinIO console: `http://localhost:9001`

Set `OPENAI_API_KEY` and rotate `API_AUTH_TOKENS` in `.env` before using model-backed chat.

## Modes

| Mode | Chat endpoint | Upload endpoint | Purpose |
| --- | --- | --- | --- |
| RAG | `/api/chat` | `/api/documents` | Direct retrieval-augmented QA. |
| RAG with sources | `/api/chat/with-sources` | `/api/documents` | Same as RAG, returns `AnswerWithSources` JSON with source citations. |
| Agent | `/api/agent/chat` | `/api/documents` | Tool-using agent over retrieved chunks. |
| LLM Wiki | `/api/wiki/chat` | `/api/wiki/upload` | Wiki-style memory over retrieved chunks. |
| LLM Wiki with sources | `/api/wiki/chat/with-sources` | `/api/wiki/upload` | Same as Wiki, returns `AnswerWithSources` JSON with source citations. |
| GBrain | `/api/gbrain/chat` | `/api/wiki/upload` | Skill layer over wiki memory. |
| Hyper | `/api/hyper/chat` | `/api/hyper/upload` | Final conversation-memory aggregation over wiki context. |
| Hyper with sources | `/api/hyper/chat/with-sources` | `/api/hyper/upload` | Same as Hyper, returns `AnswerWithSources` JSON with source citations. |
| Bot Gateway | `/api/bot/{channel}/callback` | N/A | Normalized Feishu, DingTalk, and WeChat callbacks. |

## Tenant Scoping

Uploads accept an optional `tenantId` form field. Chat and `with-sources` JSON requests accept an optional `tenantId` body field. Missing values are normalized to `default`; retrieval, wiki fallback pages, GBrain runs, and HyperMemory conversation memory are scoped by tenant. API tokens bind non-admin callers to their configured tenant, while `ADMIN` tokens can operate across tenants and run protected skill jobs.

## Runtime Configuration

| Variable | Purpose |
| --- | --- |
| `OPENAI_API_KEY` | OpenAI-compatible model provider key. |
| `OPENAI_CHAT_MODEL` | Chat model name. |
| `OPENAI_EMBEDDING_MODEL` | Embedding model name. |
| `FRONTEND_PORT` | Browser-facing frontend port. |
| `BACKEND_PORT` | Browser/API-facing backend port. |
| `MYSQL_ROOT_PASSWORD` | Local MySQL root password. |
| `SPRING_FLYWAY_ENABLED` | Enables Flyway schema migration. Defaults to `true`. |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | Allows Flyway to baseline an existing non-empty schema during first migration adoption. |
| `SPRING_FLYWAY_BASELINE_VERSION` | Baseline version for existing schemas. Defaults to `0` so V1 still runs. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Schema validation mode. Production default is `validate`; Flyway owns changes. |
| `MINIO_ROOT_USER` | Local MinIO username. |
| `MINIO_ROOT_PASSWORD` | Local MinIO password. |
| `API_AUTH_ENABLED` | Enables API token authentication for non-public API routes. Defaults to `true`. |
| `API_AUTH_TOKENS` | Semicolon-separated token specs: `name|token|tenant|ROLE,ROLE`. |
| `BOT_ENABLED` | Enables the Bot gateway. Defaults to `false`. |
| `BOT_SIGNING_SECRET` | Internal HMAC secret for normalized Bot callbacks. |
| `BOT_FEISHU_ENABLED` | Enables the Feishu channel. |
| `BOT_DINGTALK_ENABLED` | Enables the DingTalk channel. |
| `BOT_WECHAT_ENABLED` | Enables the WeChat channel. |
| `BOT_IDEMPOTENCY_ENABLED` | Enables duplicate message detection via Redis. Defaults to `true`. |
| `BOT_IDEMPOTENCY_TTL_SECONDS` | TTL in seconds for idempotency keys. Defaults to `600`. |
| `BOT_RATE_LIMIT_ENABLED` | Enables per-tenant+channel gateway rate limiting. Defaults to `true`. |
| `BOT_RATE_LIMIT_MAX_PER_MINUTE` | Max requests per window per tenant+channel. Defaults to `60`. |
| `BOT_RATE_LIMIT_WINDOW_SECONDS` | Rate limit window in seconds. Defaults to `60`. |

## Bot Gateway Smoke Test

See [Bot Integration Guide](BOT-INTEGRATION.md) for signed requests. A valid callback must include `X-Bot-Timestamp` and `X-Bot-Signature`, unless the channel-specific token header is used for a trusted internal test.

```bash
curl -i http://localhost:8080/actuator/health
curl -i -X POST http://localhost:8080/api/hyper/chat \
  -H "X-API-Token: change-me-user-token" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"default","userInput":"hello"}'
```

## Business Metrics

Custom Prometheus metrics are exposed at `/actuator/prometheus` alongside the default Spring Boot metrics.

| Metric | Type | Tags | Description |
| --- | --- | --- | --- |
| `campus.qa.operation.requests` | Counter | `operation`, `mode`, `status`, `tenant_scope` | Total QA operation attempts. `status` is `success` or `error`. |
| `campus.qa.operation.duration` | Timer | `operation`, `mode`, `status`, `tenant_scope` | Latency distribution per QA operation. |
| `campus.qa.sources.count` | DistributionSummary | `mode`, `tenant_scope` | Number of source citations returned per successful call. |

`mode` values: `rag`, `llm-wiki`, `agent`, `gbrain`, `hyper`. `tenant_scope` is `default` for blank or `default` tenants, `custom` otherwise.

Example Prometheus queries:

```promql
# P95 latency by mode
histogram_quantile(0.95, sum by (le, mode) (rate(campus_qa_operation_duration_seconds_bucket[5m])))

# Error rate by mode
sum by (mode) (rate(campus_qa_operation_requests_total{status="error"}[5m]))
  / sum by (mode) (rate(campus_qa_operation_requests_total[5m]))

# Average source count by mode
sum by (mode) (rate(campus_qa_sources_count_sum[5m]))
  / sum by (mode) (rate(campus_qa_sources_count_count[5m]))
```

## Production Checklist

- Keep HyperMemory as the single final memory aggregation layer.
- ~~Replace in-memory wiki and hyper state with durable persistence.~~ Done: wiki pages, GBrain skill runs, and HyperMemory records are persisted through JPA repositories.
- Decide whether the final runtime should stay on MySQL/Milvus/MinIO or move to SQLite-only retrieval.
- ~~Add idempotency storage for Bot message IDs before enabling platform retries.~~ Done: `BotIdempotencyService` acquires a Redis `SETNX` key by `(tenantId, channel, messageId)` before dispatch. Concurrent duplicates are ignored, successful messages keep the key until TTL expiry, and processing exceptions release the key so platform retries can run again. Missing `tenantId` defaults to `"default"`. Set `BOT_IDEMPOTENCY_ENABLED=false` to disable.
- ~~Add source citations and retrieval traces in API responses.~~ Done: `AnswerWithSources` DTO returned from `/api/chat/with-sources`, `/api/wiki/chat/with-sources`, and `/api/hyper/chat/with-sources`. Bot gateway responses (`BotMessageResponse`) now include an optional `sources` list for `rag`, `wiki`, and `hyper` modes. `agent` and `gbrain` modes remain answer-only but tenant-scoped.
- ~~Add tenant-scoped retrieval boundary.~~ Done: uploads persist `tenantId`, RAG/Wiki/Agent/GBrain/Hyper/Bot retrieval filters hydrated chunks by tenant, HyperMemory conversation memory is tenant-bucketed, and missing tenant values default to `default`.
- ~~Add RBAC for user-to-tenant membership and admin-only document namespace management.~~ Done: `ApiAuthFilter` accepts `Authorization: Bearer` or `X-API-Token`, and `AccessControlService` enforces token-bound tenant access plus `ADMIN` checks.
- ~~Replace implicit schema updates.~~ Done: Flyway baseline migration is enabled and JPA defaults to `validate`.
- ~~Add golden QA regression set to CI.~~ Done: `eval/golden/golden_cases.json` is validated by the `Golden QA regression` workflow job.
- ~~Document staging backup/restore and alert drill.~~ Done: see [Staging Runbook](STAGING-RUNBOOK.md) and `scripts/staging_drill.sh`.
- ~~Add gateway rate limits before exposing public Bot endpoints.~~ Done: `BotRateLimitService` enforces a fixed-window counter per `(tenantId, channel)` via Redis `INCR` + `EXPIRE`. Keys are scoped as `bot:rate-limit:<tenant>:<channel>:<bucket>` and auto-expire after the window. Excess requests receive `429 Too Many Requests`. Set `BOT_RATE_LIMIT_ENABLED=false` to disable. Fails open on Redis errors.

## Alert Rules

Base Prometheus alert rules are provided in [`deploy/prometheus/alerts.yml`](../deploy/prometheus/alerts.yml).
Rules are scoped with the Micrometer `application="hypermemory"` tag so they can share a Prometheus server with other services.

To enable, add the file to your Prometheus configuration:

```yaml
rule_files:
  - "deploy/prometheus/alerts.yml"
```

### Covered Alerts

| Alert | Severity | Description |
| --- | --- | --- |
| High HTTP 5xx error rate | critical | More than 5% of HTTP requests return 5xx for 5 minutes. |
| High QA error rate | warning | QA operation error rate exceeds 10% by mode for 5 minutes. |
| High p95 QA latency | warning | P95 QA latency exceeds 10 seconds by mode for 5 minutes. |
| No successful QA traffic | warning | No successful QA operations for 30 minutes. Disable for low-traffic dev environments. |
| High JVM memory pressure | warning | JVM heap usage exceeds 85% for 10 minutes. |

Thresholds are starting points. Tune them to your actual traffic patterns after initial deployment.
