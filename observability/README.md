# AOMAOI observability

The main Docker Compose stack starts the application and its metrics, logs, traces, dashboards, and alert rules together. The backend repository owns this configuration because it already orchestrates PostgreSQL, Liquibase, Spring Boot, and the sibling frontend. Observability runs as separate Compose services; it does not change wage formulas, API payloads, authorization rules, or database migrations.

## Components and data flow

```text
React/browser ──HTTP──> Spring Boot API
                           ├─ Micrometer metrics ──scrape──> Prometheus ──> Grafana
                           ├─ sampled OTLP traces ─────────> Tempo ───────> Grafana
                           └─ rolling application log file ─> Alloy ─> Loki ─> Grafana
```

- **Actuator + Micrometer** expose JVM, process, HTTP server, Hikari pool, and bounded AOMAOI operation metrics.
- **Prometheus** scrapes the app every 15 seconds. Metrics use route templates and fixed operation names; they do not use user, worker, farm, request, or financial values as labels.
- **Alloy** tails the app's dedicated log volume once. The image prepares that directory for the existing non-root app user. Alloy does not mount the Docker socket or ingest PostgreSQL, frontend, or audit-log storage.
- **Loki** stores local logs for seven days. Logs include trace/span IDs when a sampled trace exists. Application logs must not contain credentials, tokens, or sensitive financial records.
- **Tempo** receives OTLP traces and retains blocks for seven days. Its local-blocks processor serves recent TraceQL metrics queries used by Grafana Traces Drilldown; generator data stays in the Tempo volume.
- **Grafana** provisions all three data sources and four read-only dashboards. Trace IDs in logs link to Tempo, and Tempo is configured to query matching Loki logs.

This is a single-host development topology with local filesystem volumes and no service replication or authenticated internal telemetry transport. Keep the stack on a trusted development machine. The browser-facing Grafana port and host management port bind to loopback; Prometheus, Loki, and Tempo have no published host ports. This Compose setup is not a production deployment recipe.

## Start locally

Run these commands from `aomaoi-backend` in PowerShell:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
# Edit .env: set GRAFANA_ADMIN_USER and replace DB_PASSWORD, JWT_SECRET, and
# GRAFANA_ADMIN_PASSWORD with private values before first startup.
# Keep this file out of source control.
docker compose up --build -d
docker compose ps
```

The stack starts the usual app, database, migrations, and frontend as well as the monitoring services. The frontend remains at <http://localhost:3000>, the application API at <http://localhost:8081>, and Grafana at <http://localhost:3001>. Compose requires `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD` in `.env`; the example username is `admin`. Set a private password before first startup. Grafana persists its initial admin account in `grafana_data`; changing either value in `.env` later does not update that account. Keep `.env` private and do not commit it.

To reset the password for an existing Grafana volume, run `docker compose exec grafana grafana cli admin reset-admin-password "new-private-password"` from `aomaoi-backend`, then set the same `GRAFANA_ADMIN_PASSWORD` in `.env`. Use Grafana's user settings to rename an existing admin account.

Compose places management endpoints on a separate app port, published to the host only as `127.0.0.1:8083`. For local checks, use:

```powershell
Invoke-RestMethod http://localhost:8083/actuator/health
Invoke-WebRequest http://localhost:8083/actuator/prometheus
```

Prometheus scrapes `app:8083` over the private Compose network. Only `health` and `prometheus` are enabled over HTTP. The Prometheus endpoint is unauthenticated inside the private container network so Prometheus can scrape it; do not publish this port on a public interface. Kubernetes does not set `MANAGEMENT_SERVER_PORT`, so its existing app-port health probes remain unchanged. Configure a private management service/network policy and monitoring stack before enabling Prometheus access in a deployed environment.

To stop services but retain telemetry data, run the same Compose command with `down`. To stop and remove telemetry as well as the main database volume, `down -v` is destructive and removes local database data; do not use it unless you intend to erase that data.

## Dashboards

Open **Dashboards → AOMAOI** in Grafana:

1. **System overview** — target availability, request rate, 5xx rate, p95 latency, heap, CPU, and Hikari usage.
2. **API performance** — normalized route rate, response codes, latency, and slow routes.
3. **Business operations** — aggregated work-record, advance, balance, dashboard, and security outcomes. It contains no individual financial data.
4. **Logs and traces** — backend error logs and request trace exploration.

Provisioning files and dashboard JSON are version-controlled under `grafana/`. Data source UIDs (`prometheus`, `loki`, `tempo`) are stable because dashboard and trace links refer to them.

## Metrics and alerts

The backend exports standard Spring/Micrometer JVM, process, HTTP, and Hikari measurements plus these bounded counters:

- `aomaoi_business_operation_attempts_total{operation=...}`
- `aomaoi_business_operation_outcomes_total{operation=...,outcome=...}`
- `aomaoi_security_rejections_total{kind=...}`

Operation labels are fixed in `BusinessOperationMetricsInterceptor`; outcome is `success`, `rejected`, or `technical_failure`. Expected client/business rejections are not treated as technical errors. Counts are aggregated, not tied to a person. Do not add IDs, arbitrary paths, tokens, or amounts as metric labels. A success is counted after the controller returns successfully; the current service methods complete their transaction before returning.

Prometheus loads alert rules from `prometheus/alerts.yml`: sustained backend scrape failure, elevated 5xx ratio, high p95 latency, Hikari pool pressure, and repeated technical failures in selected operations. Rules evaluate locally; this stack has no configured notification receiver. Add notification credentials through deployment secret management if alert delivery is needed. No alert-delivery test is claimed here.

## Investigate a request

1. In **System overview** or **API performance**, choose a time range and identify latency, errors, or a route with unusual traffic.
2. In **Logs and traces**, query `{service="aomaoi-backend"} |= "ERROR"` or search a trace ID. Open a log's `traceId` link to Tempo.
3. In Tempo, inspect the HTTP span and its child spans, then use **Logs for this span** to view matching Loki entries.
4. Correlate aggregate operation counters for work-record, advance, and balance activity. They intentionally cannot identify a worker or expose a wage.

Trace sampling defaults to 10% through `TRACING_SAMPLING_PROBABILITY` in `.env.example`; set it to `0` to disable traces or lower it to reduce volume. A request with no sampled trace ID will still have logs and metrics but no corresponding trace. OTLP export has a five-second timeout; telemetry collection is best-effort and should not be a dependency of business success.

## Add a business metric

1. Decide whether the event is an operational outcome worth aggregating and whether it is an expected rejection or technical failure.
2. Add only a bounded operation/outcome name. Never include a worker, farm, user, request, token, or financial amount in a tag.
3. Instrument at a boundary that avoids counting nested calls twice and reflects the operation's completion semantics.
4. Add a focused unit test for the expected counter and outcome.
5. Add a dashboard panel or alert only when it answers a specific operational question. Verify the resulting metric name in `/actuator/prometheus` before writing PromQL.

## Verification and limits

Run the backend test suite with `./mvnw test` (Windows: `./mvnw.cmd test`). Validate the full Compose file with `docker compose config`, then start it and inspect `ps`, logs, Prometheus **Targets**, Grafana data-source health, dashboard panels, a real request trace, and its correlated logs.

Actual end-to-end checks require a working Docker daemon. Unit tests and static checks do not prove that containers start, scrape, export, or correlate. This implementation does not add browser-side telemetry: the current frontend has no trusted telemetry ingestion/authentication boundary, and introducing one would add CORS, privacy, and availability concerns. The backend stack provides operational visibility without changing Axios, JWT, Zustand, or page behavior. Consider browser telemetry separately only after defining its data minimization, access controls, sampling, and ingestion design.
