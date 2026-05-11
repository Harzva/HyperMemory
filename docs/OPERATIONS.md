# Operations Guide

## Local Runtime

```bash
cp .env.example .env
docker compose up -d --build
```

Check:

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

## Modes

The frontend can route chat and upload requests to multiple backend modes:

| Mode | Chat endpoint | Upload endpoint |
| --- | --- | --- |
| RAG | `/api/chat` | `/api/documents` |
| Agent | `/api/agent/chat` | `/api/documents` |
| LLM Wiki | `/api/wiki/chat` | `/api/wiki/upload` |
| GBrain | `/api/gbrain/chat` | `/api/wiki/upload` |
| Hierarchy Memory | `/api/hierarchy/chat` | `/api/hierarchy/upload` |
| Hyper Memory | `/api/hyper/chat` | `/api/hyper/upload` |

## Configuration

Use `.env` for runtime settings. Do not commit real API keys.

| Variable | Purpose |
| --- | --- |
| `OPENAI_API_KEY` | Model provider key. |
| `FRONTEND_PORT` | Browser-facing frontend port. |
| `BACKEND_PORT` | Browser/API-facing backend port. |
| `MYSQL_ROOT_PASSWORD` | MySQL root password for local stack. |
| `MINIO_ROOT_USER` | MinIO console/access username. |
| `MINIO_ROOT_PASSWORD` | MinIO password. |

## Production Readiness Checklist

- Persist wiki, hierarchy, and hyper memory outside JVM memory.
- Add authentication and workspace isolation.
- Add source citations and retrieval traces.
- Add CI builds and integration smoke tests.
- Add observability for API latency, retrieval latency, and model cost.

