# HyperMemory

HyperMemory is a memory-enhanced AI knowledge system built from the final version of the Campus QA evolution. It is positioned as a general-purpose product foundation rather than a campus-only demo.

## What It Includes

- RAG document ingestion and retrieval.
- Agent-style question answering.
- LLM Wiki memory.
- GBrain skill concepts.
- Hierarchy memory.
- Hyper memory.
- Vue 3 chat and upload frontend.
- Docker Compose deployment stack.

## Stack

- Java 17
- Spring Boot 3.3.0
- LangChain4j
- MySQL
- Redis
- Milvus
- MinIO
- Vue 3
- Vite

## Run

```bash
docker compose up -d
```

Open the frontend at `http://localhost:3000`.

The backend runs on `http://localhost:8080`.

## Configuration

The sample config uses a dummy OpenAI key. Set a real API key before using the model-backed chat flows:

```yaml
openai:
  api-key: your-key
```

or update `OPENAI_API_KEY` in `docker-compose.yml`.

## Notes

This codebase is a runnable product prototype. The next production steps are proper document chunking, durable memory persistence, real streaming, multi-tenant isolation, stronger retrieval, observability, and authentication.

