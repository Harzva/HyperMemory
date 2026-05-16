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

Set `OPENAI_API_KEY` in `.env` before using model-backed chat.

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

Uploads accept an optional `tenantId` form field. Chat and `with-sources` JSON requests accept an optional `tenantId` body field. Missing values are normalized to `default`; retrieval, wiki fallback pages, and HyperMemory conversation memory are scoped by tenant.

## Runtime Configuration

| Variable | Purpose |
| --- | --- |
| `OPENAI_API_KEY` | OpenAI-compatible model provider key. |
| `OPENAI_CHAT_MODEL` | Chat model name. |
| `OPENAI_EMBEDDING_MODEL` | Embedding model name. |
| `FRONTEND_PORT` | Browser-facing frontend port. |
| `BACKEND_PORT` | Browser/API-facing backend port. |
| `MYSQL_ROOT_PASSWORD` | Local MySQL root password. |
| `MINIO_ROOT_USER` | Local MinIO username. |
| `MINIO_ROOT_PASSWORD` | Local MinIO password. |
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
```

## Production Checklist

- Keep HyperMemory as the single final memory aggregation layer.
- Replace in-memory wiki and hyper state with durable persistence.
- Decide whether the final runtime should stay on MySQL/Milvus/MinIO or move to SQLite-only retrieval.
- ~~Add idempotency storage for Bot message IDs before enabling platform retries.~~ Done: `BotIdempotencyService` acquires a Redis `SETNX` key by `(tenantId, channel, messageId)` before dispatch. Concurrent duplicates are ignored, successful messages keep the key until TTL expiry, and processing exceptions release the key so platform retries can run again. Missing `tenantId` defaults to `"default"`. Set `BOT_IDEMPOTENCY_ENABLED=false` to disable.
- ~~Add source citations and retrieval traces in API responses.~~ Done: `AnswerWithSources` DTO returned from `/api/chat/with-sources`, `/api/wiki/chat/with-sources`, and `/api/hyper/chat/with-sources`. Bot gateway responses (`BotMessageResponse`) now include an optional `sources` list for `rag`, `wiki`, and `hyper` modes. `agent` and `gbrain` modes remain answer-only but tenant-scoped.
- ~~Add tenant-scoped retrieval boundary.~~ Done: uploads persist `tenantId`, RAG/Wiki/Agent/GBrain/Hyper/Bot retrieval filters hydrated chunks by tenant, HyperMemory conversation memory is tenant-bucketed, and missing tenant values default to `default`.
- Add RBAC for user-to-tenant membership and admin-only document namespace management.
- ~~Add gateway rate limits before exposing public Bot endpoints.~~ Done: `BotRateLimitService` enforces a fixed-window counter per `(tenantId, channel)` via Redis `INCR` + `EXPIRE`. Keys are scoped as `bot:rate-limit:<tenant>:<channel>:<bucket>` and auto-expire after the window. Excess requests receive `429 Too Many Requests`. Set `BOT_RATE_LIMIT_ENABLED=false` to disable. Fails open on Redis errors.
