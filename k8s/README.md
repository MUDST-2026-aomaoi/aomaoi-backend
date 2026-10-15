# Kubernetes + CI/CD ของ aomaoi-backend

Stack: backend (Deployment x2) + Postgres (StatefulSet + PVC) + Redis + Ingress path `/aomaoi-api`

```
k8s/data     namespace, ConfigMap, Postgres StatefulSet (headless Service), Redis
k8s/migrate  Job รัน Liquibase + seed หนึ่งครั้งต่อการ deploy
k8s/app      backend Deployment + Service + Ingress
scripts/k8s-deploy.sh   deploy ทั้งหมดตามลำดับ (secret -> data -> migrate -> app)
scripts/k8s-smoke.sh    ทดสอบ health / login / GET /api/farms ผ่าน Ingress
```

## รันบนเครื่องตัวเอง (ทุกคนทำได้)

ต้องมี: Docker, `kubectl`, cluster (Docker Desktop Kubernetes, kind หรือ minikube), ingress controller, bash (Git Bash บน Windows ใช้ได้)

```bash
# 1) ingress-nginx (ครั้งเดียว) -- ถ้ามี controller อื่นอยู่แล้ว ข้ามได้ แล้วตั้ง INGRESS_CLASS ตอน deploy
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.2/deploy/static/provider/cloud/deploy.yaml
kubectl -n ingress-nginx rollout status deployment/ingress-nginx-controller

# 2) build image จากโค้ดในเครื่อง
docker build -t aomaoi-backend:local .
#    kind เท่านั้น: kind load docker-image aomaoi-backend:local

# 3) deploy (สร้าง Secret สุ่มให้เองครั้งแรก)
BACKEND_IMAGE=aomaoi-backend:local scripts/k8s-deploy.sh

# 4) เปิดทางเข้า แล้วทดสอบ
kubectl -n ingress-nginx port-forward svc/ingress-nginx-controller 16969:80 &
scripts/k8s-smoke.sh            # http://localhost:16969/aomaoi-api
```

ใช้ image จาก CI แทนการ build เอง: `scripts/k8s-deploy.sh` (default `ghcr.io/mudst-2026-aomaoi/aomaoi-backend:latest`)
ถ้า package เป็น private ให้สร้าง pull secret ชื่อ `ghcr-pull` ใน namespace ก่อน

ตัวแปรของ `k8s-deploy.sh`: `NAMESPACE` (default `aomaoi-app`), `BACKEND_IMAGE`, `INGRESS_CLASS` (default `nginx`), `DEMO_DATA` (`true` = seed demo user รหัส `1234`, default `false` = สร้างแค่ `superadmin`), `KUBE_CONTEXT`

รหัส `superadmin` (สุ่มตอนสร้าง Secret):

```bash
kubectl -n aomaoi-app get secret aomaoi-secrets -o jsonpath='{.data.bootstrap-admin-password}' | base64 -d
```

Secret ไม่ถูกเก็บใน git และ deploy รอบถัดไปไม่เปลี่ยนค่าเดิม (เปลี่ยนรหัส DB หลัง Postgres สร้างแล้วจะทำให้แอปต่อไม่ได้)

## CI/CD (`.github/workflows/build.yml`)

| job | ทำอะไร | เมื่อไหร่ |
|---|---|---|
| test | `./mvnw test`, parse script, render manifest | ทุก push/PR ที่ main, dev |
| build | build image, push GHCR tag `<sha>` + tag branch (`main` -> `latest`) | PR build อย่างเดียวไม่ push |
| deploy-kind | สร้าง kind cluster ชั่วคราว, ลง ingress-nginx, `k8s-deploy.sh`, `k8s-smoke.sh`, ลบ Pod DB แล้วเช็คข้อมูลยังอยู่ | ทุกครั้ง ไม่ต้องมี secret |
| deploy | deploy image `<sha>` ไป cluster จริงด้วย `k8s-deploy.sh` | push main หรือรันเอง และต้องมี `KUBE_CONFIG` |

เปิด deploy ไป cluster จริง (ตั้งใน GitHub repo settings):

- Secret `KUBE_CONFIG` = `base64 -w0 kubeconfig` ของ cluster ที่ GitHub runner เข้าถึงได้
- Variable (ไม่บังคับ) `KUBE_NAMESPACE`, `INGRESS_CLASS`, `DEPLOY_RUNNER` (label ของ self-hosted runner ถ้า cluster อยู่ในเครื่อง/เครือข่ายส่วนตัว)
- ไม่ตั้ง `KUBE_CONFIG` = job `deploy` ข้ามไปเฉยๆ pipeline ไม่แดง
- ถ้าต้องการให้ต้องกดอนุมัติก่อน deploy ให้ตั้ง required reviewers ที่ Environment `production`

## ตรวจสอบ / ปัญหาที่เจอ

- ดูสถานะ: `kubectl -n aomaoi-app get pods,pvc,ingress`; log migration: `kubectl -n aomaoi-app logs job/aomaoi-migrate`
- Ingress ของ controller เดียวกันที่มี path `/aomaoi-api` ซ้ำกันหลาย namespace จะชนกัน (ตอบจากตัวใดตัวหนึ่ง) ลบ namespace เก่าก่อน หรือใช้ `kubectl -n aomaoi-app port-forward svc/aomaoi-backend 8080:8080` แล้ว `BASE_URL=http://localhost:8080/aomaoi-api scripts/k8s-smoke.sh`
- ลบทั้งหมดรวมข้อมูล DB: `kubectl delete namespace aomaoi-app` (PVC หายด้วย)
