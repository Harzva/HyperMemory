# Staging Runbook

This runbook closes the minimum staging drill before production promotion:
auth, Flyway migration, backup/restore, golden QA, and alert delivery.

## Required Environment

| Variable | Example | Purpose |
| --- | --- | --- |
| `BASE_URL` | `https://hypermemory-staging.example.com` | Staging backend URL. |
| `API_TOKEN` | `...` | Token with `USER` or `ADMIN` role from `API_AUTH_TOKENS`. |
| `MYSQL_HOST` | `127.0.0.1` | MySQL host reachable from the shell. |
| `MYSQL_PORT` | `3306` | MySQL port. |
| `MYSQL_USER` | `root` | MySQL user with backup permissions. |
| `MYSQL_PASSWORD` | `...` | MySQL password. |
| `MYSQL_DATABASE` | `rag` | Source database. |
| `RESTORE_DATABASE` | `rag_restore_drill` | Isolated restore target database. |
| `ALERTMANAGER_URL` | `https://alertmanager.example.com` | Optional alert delivery endpoint. |

## Promotion Gate

1. Deploy the staging image with `SPRING_FLYWAY_ENABLED=true` and `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.
2. Rotate `API_AUTH_TOKENS`; do not use `change-me-*` outside local demos.
3. Run `python scripts/run_golden_eval.py eval/golden/golden_cases.json`.
4. Run `bash scripts/staging_drill.sh`.
5. Confirm Flyway created or updated `flyway_schema_history`.
6. Confirm the backup file exists under `build/staging-backups/`.
7. Restore only into `RESTORE_DATABASE`, never into the live staging DB.
8. Send one synthetic Alertmanager alert and confirm the notification route.
9. Check `/actuator/prometheus` and Prometheus targets for the application label `hypermemory`.

## Frontend Auth

The workbench can carry a token through `VITE_API_TOKEN`, localStorage key
`campusQaApiToken`, or a one-time staging URL parameter:

```text
https://hypermemory-staging.example.com?apiToken=<rotated-user-token>
```

The URL parameter is for staging smoke tests only. Use platform login or a
gateway session before exposing the frontend broadly.

## Manual Checks

```bash
curl -fsS "$BASE_URL/actuator/health"
curl -i -X POST "$BASE_URL/api/hyper/chat" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"staging","userInput":"auth smoke"}'
```

The unauthenticated request must return `401`.
