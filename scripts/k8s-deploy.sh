#!/usr/bin/env bash
# Deploy the backend to Kubernetes with a persistent PostgreSQL StatefulSet.
# Works the same on a laptop (Docker Desktop, kind, minikube) and in CI.
#
#   scripts/k8s-deploy.sh                                   # image ghcr.io/.../aomaoi-backend:latest
#   BACKEND_IMAGE=aomaoi-backend:local scripts/k8s-deploy.sh # locally built image
#
# Environment (all optional):
#   NAMESPACE        target namespace                          (default: aomaoi-app)
#   BACKEND_IMAGE    full image reference to deploy            (default: ghcr.io/mudst-2026-aomaoi/aomaoi-backend:latest)
#   INGRESS_CLASS    IngressClass of your controller           (default: nginx)
#   DEMO_DATA        true = seed demo accounts, false = only superadmin (default: false)
#   KUBE_CONTEXT     kubectl context to use                    (default: current context)
#   ROLLOUT_TIMEOUT  seconds to wait per rollout; raise on slow image pulls (default: 600)
set -euo pipefail

cd "$(dirname "$0")/.."

NAMESPACE="${NAMESPACE:-aomaoi-app}"
DEFAULT_IMAGE="ghcr.io/mudst-2026-aomaoi/aomaoi-backend:latest"
BACKEND_IMAGE="${BACKEND_IMAGE:-$DEFAULT_IMAGE}"
INGRESS_CLASS="${INGRESS_CLASS:-nginx}"
DEMO_DATA="${DEMO_DATA:-false}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-600}"
KUBECTL=(kubectl)
if [ -n "${KUBE_CONTEXT:-}" ]; then
  KUBECTL+=(--context "$KUBE_CONTEXT")
fi
k() { "${KUBECTL[@]}" "$@"; }

if ! [[ "$NAMESPACE" =~ ^[a-z0-9]([-a-z0-9]*[a-z0-9])?$ ]]; then
  echo "Invalid NAMESPACE '$NAMESPACE'" >&2; exit 1
fi
if ! [[ "$BACKEND_IMAGE" =~ ^[a-zA-Z0-9][a-zA-Z0-9./:@_-]*$ ]]; then
  echo "Invalid BACKEND_IMAGE '$BACKEND_IMAGE'" >&2; exit 1
fi
if ! [[ "$INGRESS_CLASS" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*$ ]]; then
  echo "Invalid INGRESS_CLASS '$INGRESS_CLASS'" >&2; exit 1
fi
if [[ "$DEMO_DATA" != true && "$DEMO_DATA" != false ]]; then
  echo "Invalid DEMO_DATA '$DEMO_DATA' (expected true or false)" >&2; exit 1
fi
LIQUIBASE_CONTEXTS=production
if [ "$DEMO_DATA" = true ]; then LIQUIBASE_CONTEXTS=demo; fi

# Render a kustomization and apply the per-deployment settings.
render() {
  k kustomize "k8s/$1" \
    | sed -e "s#$DEFAULT_IMAGE#$BACKEND_IMAGE#g" \
          -e "s#aomaoi-app#$NAMESPACE#g" \
          -e "s#ingressClassName: nginx#ingressClassName: $INGRESS_CLASS#" \
          -e "s#SPRING_LIQUIBASE_CONTEXTS: .*#SPRING_LIQUIBASE_CONTEXTS: '$LIQUIBASE_CONTEXTS'#" \
          -e "s#APP_SEED_DEMO_DATA: \"false\"#APP_SEED_DEMO_DATA: \"$DEMO_DATA\"#"
}

random_value() {
  # 48 random bytes, base64. openssl is on every dev machine; fall back to /dev/urandom.
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 48 | tr -d '\r\n'
  else
    head -c 48 /dev/urandom | base64 | tr -d '\r\n'
  fi
}

echo "==> Cluster: $(k config current-context)  namespace: $NAMESPACE  image: $BACKEND_IMAGE"
k cluster-info >/dev/null

echo "==> Namespace and application configuration"
k get namespace "$NAMESPACE" >/dev/null 2>&1 || k create namespace "$NAMESPACE"

# Keep database credentials stable across deploys, including clusters with an
# existing application secret created before the PostgreSQL service was restored.
if ! k -n "$NAMESPACE" get secret aomaoi-postgres-db >/dev/null 2>&1; then
  echo "==> Creating PostgreSQL credentials"
  k -n "$NAMESPACE" create secret generic aomaoi-postgres-db \
    --from-literal=database-user=aomaoi_user \
    --from-literal=database-password="$(random_value)"
fi

# Secrets are never stored in git. Create them once; later deploys keep the existing values
# (changing the DB password after Postgres initialised would lock the app out).
if ! k -n "$NAMESPACE" get secret aomaoi-secrets >/dev/null 2>&1; then
  echo "==> Creating Secret aomaoi-secrets with random values"
  k -n "$NAMESPACE" create secret generic aomaoi-secrets \
    --from-literal=jwt-secret="$(random_value)" \
    --from-literal=bootstrap-admin-password="$(random_value)"
fi

render data | k apply -f -
k -n "$NAMESPACE" rollout status statefulset/postgres --timeout="${ROLLOUT_TIMEOUT}s"
echo "==> Database migration + seed (one-off Job)"
k -n "$NAMESPACE" delete job aomaoi-migrate --ignore-not-found --wait=true
render migrate | k apply -f -
deadline=$((SECONDS + 600))
while true; do
  succeeded="$(k -n "$NAMESPACE" get job aomaoi-migrate -o 'jsonpath={.status.succeeded}')"
  failed="$(k -n "$NAMESPACE" get job aomaoi-migrate -o 'jsonpath={.status.failed}')"
  if [ "${succeeded:-0}" -ge 1 ]; then
    break
  fi
  if [ "${failed:-0}" -ge 3 ] || [ "$SECONDS" -ge "$deadline" ]; then
    echo "Migration Job failed. Logs:" >&2
    k -n "$NAMESPACE" logs job/aomaoi-migrate --all-containers --tail=100 >&2 || true
    exit 1
  fi
  sleep 3
done
echo "Migration finished."

echo "==> Backend Deployment + Ingress"
# The admission webhook of some ingress controllers is not ready right after install; retry briefly.
for attempt in 1 2 3 4 5 6; do
  if render app | k apply -f -; then
    break
  fi
  if [ "$attempt" -eq 6 ]; then echo "Applying app manifests failed." >&2; exit 1; fi
  sleep 10
done
if ! k -n "$NAMESPACE" rollout status deployment/aomaoi-backend --timeout="${ROLLOUT_TIMEOUT}s"; then
  k -n "$NAMESPACE" describe pods -l app=aomaoi-backend >&2 || true
  k -n "$NAMESPACE" logs -l app=aomaoi-backend --tail=80 >&2 || true
  exit 1
fi

actual="$(k -n "$NAMESPACE" get deployment aomaoi-backend -o 'jsonpath={.spec.template.spec.containers[0].image}')"
if [ "$actual" != "$BACKEND_IMAGE" ]; then
  echo "Deployment uses '$actual' but '$BACKEND_IMAGE' was requested." >&2
  exit 1
fi

cat <<EOF

Deployed $actual to namespace $NAMESPACE.
  Health:   curl http://localhost:16969/aomaoi-api/actuator/health   (after: kubectl -n <ingress-ns> port-forward svc/<controller> 16969:80)
  Password: kubectl -n $NAMESPACE get secret aomaoi-secrets -o jsonpath='{.data.bootstrap-admin-password}' | base64 -d   (user: superadmin)
EOF
