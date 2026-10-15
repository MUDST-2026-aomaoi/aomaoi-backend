#!/usr/bin/env bash
# Database-backed smoke test through the Ingress: health, superadmin login, authenticated read.
#
#   scripts/k8s-smoke.sh                                  # http://localhost:16969/aomaoi-api
#   BASE_URL=http://localhost/aomaoi-api scripts/k8s-smoke.sh
#
# Environment (all optional):
#   BASE_URL         URL of the backend through the Ingress   (default: http://localhost:16969/aomaoi-api)
#   NAMESPACE        namespace holding aomaoi-secrets         (default: aomaoi-app)
#   SMOKE_PASSWORD   superadmin password                      (default: read from the cluster Secret)
#   KUBE_CONTEXT     kubectl context to use
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:16969/aomaoi-api}"
NAMESPACE="${NAMESPACE:-aomaoi-app}"

KUBECTL=(kubectl)
if [ -n "${KUBE_CONTEXT:-}" ]; then
  KUBECTL+=(--context "$KUBE_CONTEXT")
fi

PASSWORD="${SMOKE_PASSWORD:-}"
if [ -z "$PASSWORD" ]; then
  PASSWORD="$("${KUBECTL[@]}" -n "$NAMESPACE" get secret aomaoi-secrets -o 'jsonpath={.data.bootstrap-admin-password}' | base64 -d)"
fi

# Retry only transport errors and 502/503/504 (Ingress endpoints still converging).
# Auth failures and application errors are returned immediately.
request() {
  local out status
  for attempt in $(seq 1 20); do
    out="$(curl -sS -m 10 -w '\n%{http_code}' "$@" 2>/dev/null)" || out=$'\n000'
    status="${out##*$'\n'}"
    case "$status" in
      000|502|503|504) sleep 3 ;;
      *) printf '%s' "$out"; return 0 ;;
    esac
  done
  printf '%s' "$out"
}
body_of() { printf '%s' "${1%$'\n'*}"; }
status_of() { printf '%s' "${1##*$'\n'}"; }

echo "==> readiness: $BASE_URL/actuator/health/readiness"
res="$(request "$BASE_URL/actuator/health/readiness")"
[ "$(status_of "$res")" = "200" ] || { echo "readiness failed: HTTP $(status_of "$res")" >&2; exit 1; }
grep -q '"status":"UP"' <<<"$(body_of "$res")" || { echo "readiness not UP: $(body_of "$res")" >&2; exit 1; }

echo "==> login as superadmin"
res="$(request -X POST -H 'Content-Type: application/json' \
  --data "{\"username\":\"superadmin\",\"password\":\"$PASSWORD\"}" "$BASE_URL/api/auth/login")"
[ "$(status_of "$res")" = "200" ] || { echo "login failed: HTTP $(status_of "$res")" >&2; exit 1; }
TOKEN="$(body_of "$res" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
[ -n "$TOKEN" ] || { echo "login returned no token" >&2; exit 1; }

echo "==> wrong password must be rejected"
res="$(request -X POST -H 'Content-Type: application/json' \
  --data '{"username":"superadmin","password":"definitely-wrong"}' "$BASE_URL/api/auth/login")"
[ "$(status_of "$res")" = "401" ] || { echo "expected 401, got HTTP $(status_of "$res")" >&2; exit 1; }

echo "==> unauthenticated request must be rejected"
res="$(request "$BASE_URL/api/farms")"
case "$(status_of "$res")" in 401|403) ;; *) echo "expected 401/403, got HTTP $(status_of "$res")" >&2; exit 1;; esac

echo "==> database-backed read: GET /api/farms"
res="$(request -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/farms")"
[ "$(status_of "$res")" = "200" ] || { echo "GET /api/farms failed: HTTP $(status_of "$res")" >&2; exit 1; }

echo "Smoke test passed."
