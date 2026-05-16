#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BACKUP_DIR="${BACKUP_DIR:-build/staging-backups}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-rag}"
RESTORE_DATABASE="${RESTORE_DATABASE:-rag_restore_drill}"
APP_NAME="hypermemory"

mkdir -p "$BACKUP_DIR"

echo "[1/5] health"
curl -fsS "$BASE_URL/actuator/health" >/dev/null

echo "[2/5] auth rejects missing token"
status="$(curl -sS -o /tmp/campus_qa_auth_check.out -w "%{http_code}" \
  -X POST "$BASE_URL/api/hyper/chat" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"staging","userInput":"auth smoke"}')"
test "$status" = "401"

echo "[3/5] metrics endpoint"
curl -fsS "$BASE_URL/actuator/prometheus" | grep -q "jvm_memory_used_bytes"

if command -v mysqldump >/dev/null 2>&1 && [ -n "${MYSQL_HOST:-}" ] && [ -n "${MYSQL_USER:-}" ] && [ -n "${MYSQL_PASSWORD:-}" ]; then
  echo "[4/5] mysql backup"
  backup_file="$BACKUP_DIR/${APP_NAME}-$(date +%Y%m%d%H%M%S).sql"
  mysqldump -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" > "$backup_file"
  test -s "$backup_file"

  if [ "${CONFIRM_RESTORE:-false}" = "true" ]; then
    test "$RESTORE_DATABASE" != "$MYSQL_DATABASE"
    mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" \
      -e "CREATE DATABASE IF NOT EXISTS \`$RESTORE_DATABASE\`"
    mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$RESTORE_DATABASE" < "$backup_file"
  else
    echo "restore skipped; set CONFIRM_RESTORE=true to restore into $RESTORE_DATABASE"
  fi
else
  echo "[4/5] mysql backup skipped; install mysqldump and set MYSQL_HOST/MYSQL_USER/MYSQL_PASSWORD"
fi

if [ -n "${ALERTMANAGER_URL:-}" ]; then
  echo "[5/5] synthetic alert"
  curl -fsS -X POST "$ALERTMANAGER_URL/api/v2/alerts" \
    -H "Content-Type: application/json" \
    -d "[{\"labels\":{\"alertname\":\"CampusQaStagingDrill\",\"service\":\"$APP_NAME\",\"severity\":\"info\"},\"annotations\":{\"summary\":\"staging alert drill\"}}]" >/dev/null
else
  echo "[5/5] alert skipped; set ALERTMANAGER_URL to test notification delivery"
fi

echo "staging drill completed for $APP_NAME"
