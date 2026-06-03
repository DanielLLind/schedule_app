#!/usr/bin/env bash
set -euo pipefail

PORT=${1:-8081}
BUILD=${2:-build}

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"
PID_FILE="$ROOT_DIR/demo-api.pid"

is_port_free() {
  local port=$1
  python - <<'PY' "$port"
import socket, sys
port = int(sys.argv[1])
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
try:
    s.bind(('127.0.0.1', port))
    s.close()
    sys.exit(0)
except OSError:
    sys.exit(1)
PY
}

find_free_port() {
  local candidate=$1
  local max=$((candidate + 50))
  while [[ $candidate -le $max ]]; do
    if is_port_free "$candidate"; then
      echo "$candidate"
      return 0
    fi
    candidate=$((candidate + 1))
  done
  return 1
}

cleanup() {
  if [[ -f "$PID_FILE" ]]; then
    local old_pid
    old_pid=$(<"$PID_FILE")
    if [[ -n "$old_pid" ]] && kill -0 "$old_pid" 2>/dev/null; then
      echo "Stopping previous demo server PID $old_pid..."
      kill "$old_pid" 2>/dev/null || true
      sleep 1
    fi
    rm -f "$PID_FILE"
  fi
}
trap cleanup EXIT INT TERM

REQUESTED_PORT=${1:-8081}
PORT=$(find_free_port "$REQUESTED_PORT")
if [[ -z "$PORT" ]]; then
  echo "Could not find a free port starting at $REQUESTED_PORT"
  exit 1
fi
if [[ "$PORT" != "$REQUESTED_PORT" ]]; then
  echo "Port $REQUESTED_PORT is busy, using $PORT instead."
fi

if [[ "$BUILD" != "skip" ]]; then
  echo "Building project..."
  mvnd clean package
fi

if command -v lsof >/dev/null 2>&1; then
  if lsof -iTCP:"$PORT" -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "Port $PORT is already in use. Stop the process or choose another port."
    exit 1
  fi
else
  echo "Warning: lsof not installed; skipping port in-use check."
fi

start_server() {
  echo "Starting Spring Boot on port $PORT..."
  if [[ -f "$PID_FILE" ]]; then
    local existing_pid
    existing_pid=$(<"$PID_FILE")
    if [[ -n "$existing_pid" ]] && kill -0 "$existing_pid" 2>/dev/null; then
      echo "Stopping stale demo server PID $existing_pid..."
      kill "$existing_pid" 2>/dev/null || true
      sleep 1
    fi
    rm -f "$PID_FILE"
  fi

  mvnd -Dspring-boot.run.arguments="--server.port=$PORT" spring-boot:run > demo-api.log 2>&1 &
  SERVER_PID=$!
  echo "$SERVER_PID" > "$PID_FILE"
}

wait_for_port() {
  local max=30
  local count=0
  while [[ $count -lt $max ]]; do
    if command -v nc >/dev/null 2>&1; then
      if nc -z localhost "$PORT" >/dev/null 2>&1; then
        return 0
      fi
    elif command -v curl >/dev/null 2>&1; then
      if curl --silent --output /dev/null --connect-timeout 2 "http://localhost:$PORT/" >/dev/null 2>&1; then
        return 0
      fi
    else
      echo "No nc or curl available to probe the port."
      return 1
    fi
    count=$((count + 1))
    echo "Waiting for port $PORT... ($count/$max)"
    sleep 1
  done
  return 1
}

invoke_json() {
  local method=$1
  local path=$2
  local body=${3:-}
  echo
  echo "$method http://localhost:$PORT$path"
  if [[ -n "$body" ]]; then
    echo "$body"
  fi
  if [[ -n "$body" ]]; then
    curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X "$method" \
      -H 'Content-Type: application/json' \
      -d "$body" \
      "http://localhost:$PORT$path"
  else
    curl -sS -w "\nHTTP_STATUS:%{http_code}\n" "http://localhost:$PORT$path"
  fi
}

start_server

if ! wait_for_port; then
  echo "Server did not become available on port $PORT. See demo-api.log"
  exit 1
fi

echo "Server is ready. Running demo requests..."

invoke_json POST /persons '{"name":"Alice","email":"alice@example.com"}'
invoke_json POST /persons '{"name":"Alice","email":"alice@example.com"}'
invoke_json GET /persons/alice@example.com

meeting_response=$(invoke_json POST /meetings '{"startTime":"2024-06-01T09:00","participantEmails":["alice@example.com"]}')
meeting_body=$(printf '%s' "$meeting_response" | python - <<'PY'
import sys
body = ''.join(line for line in sys.stdin if not line.startswith('HTTP_STATUS:'))
print(body, end='')
PY
)
meeting_id=$(printf '%s' "$meeting_body" | python - <<'PY'
import sys, json, re
body = sys.stdin.read()
m = re.search(r'\{.*\}', body, re.S)
print(json.loads(m.group(0))['id'] if m else '')
PY
)

if [[ -n "$meeting_id" ]]; then
  invoke_json GET "/meetings/$meeting_id"
fi

invoke_json GET /persons/alice@example.com/schedule
invoke_json POST /meetings/suggest '{"participantEmails":["alice@example.com"],"from":"2024-06-01T08:00","to":"2024-06-01T11:00"}'


echo
echo "Demo complete. Server PID: $SERVER_PID"
echo "Stop it with: kill $SERVER_PID"
