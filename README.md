# AOMAOI Backend

ระบบ backend สำหรับ **AOMAOI ระบบจัดการฟาร์มอ้อยและบันทึกค่าแรงคนงาน** ให้บริการ REST API สำหรับ frontend จัดการบัญชีผู้ใช้ ฟาร์ม โปรไฟล์ผู้ดูแลและคนงาน บันทึกงาน รายงานภาพรวม และประวัติการใช้งาน

README นี้อธิบายวิธีเปิดระบบในเครื่อง โครงสร้างโค้ด ฐานข้อมูลและ migration การยืนยันตัวตน API หลัก การทดสอบ และการ deploy

## ความสามารถหลัก

- เข้าสู่ระบบด้วย username/password และใช้ JWT ยืนยันตัวตนกับ API ที่ต้องล็อกอิน
- รองรับบทบาท `superadmin`, `admin` และ `worker` พร้อมกำหนดสิทธิ์ตามบทบาท
- จัดการฟาร์ม บัญชีผู้ดูแลฟาร์ม และบัญชีคนงาน
- สร้างและดูบันทึกงาน 4 ประเภท: ตัดอ้อย (`cutting`), ปลูกอ้อย (`planting`), รดน้ำ (`watering`) และพ่นยา (`spraying`); backend คำนวณค่าจ้างจากข้อมูลของแต่ละประเภท
- ดูข้อมูลภาพรวม เช่น จำนวนคนงาน จำนวนบันทึกงาน และยอดค่าจ้าง
- บันทึก audit log สำหรับกิจกรรมสำคัญ เช่น การรีเซ็ตรหัสผ่าน
- ใช้ Liquibase ดูแล schema และข้อมูลตัวอย่างผ่าน migration แบบมีลำดับ
- มีระบบติดตามและ cleanup ข้อมูล Cypress E2E ที่ทำงานบน stack และฐานข้อมูลเดียวกับแอป

## เทคโนโลยี

| ส่วน | เทคโนโลยี |
|---|---|
| ภาษาและ runtime | Java 21, Spring Boot |
| REST API | Spring Web MVC |
| ORM และเชื่อมฐานข้อมูล | Spring Data JPA / Hibernate |
| ฐานข้อมูล | PostgreSQL 15 |
| Schema migration | Liquibase |
| Authentication | Spring Security, JWT (JJWT), BCrypt |
| Build และ unit test | Maven Wrapper, JUnit, Spring test |
| Local stack | Dockerfile, Docker Compose |
| Deploy | Kubernetes manifests, Kustomize, GitHub Actions |

## ความต้องการก่อนเริ่ม

สำหรับการรันทั้งระบบด้วย Docker ต้องติดตั้ง Docker Desktop พร้อม Docker Compose และมีโฟลเดอร์ `aomaoi-frontend` อยู่ข้าง `aomaoi-backend` เพราะ Compose ของ backend build frontend จาก `../aomaoi-frontend` ด้วย

สำหรับการรัน Java โดยตรง ต้องมี Java 21 และใช้ PostgreSQL ที่เข้าถึงได้จากเครื่อง

## เริ่มระบบด้วย Docker Compose (แนะนำ)

Compose ของ backend เปิดระบบครบชุด: PostgreSQL, migration job ของ Liquibase, backend API และ frontend ซึ่ง build จาก repository frontend ที่อยู่ข้างกัน

1. เปิด PowerShell หรือ terminal ในโฟลเดอร์ backend แล้วสร้างไฟล์ `.env` จากตัวอย่าง:

   ```powershell
   Copy-Item .env.example .env
   ```

2. แก้ `.env` ก่อนเริ่ม โดยอย่างน้อยควรเปลี่ยน `DB_PASSWORD` และ `JWT_SECRET` ให้เป็นค่าที่คาดเดายากและไม่ซ้ำกับที่อื่น ค่า `JWT_SECRET` ต้องยาวอย่างน้อย 32 bytes สำหรับ HS256 อย่า commit หรือส่งไฟล์ `.env` ให้ผู้อื่น

3. สั่ง build และเปิดทั้งระบบ:

   ```powershell
   docker compose up --build -d
   ```

4. ตรวจสถานะและ log:

   ```powershell
   docker compose ps -a
   docker compose logs --tail 100 migrate app
   ```

เมื่อ migration ทำงานสำเร็จ API จะเริ่มให้บริการ ตรวจสอบได้ที่:

- Frontend: <http://localhost:3000>
- Backend API: <http://localhost:8081>
- Health check: <http://localhost:8081/actuator/health>
- PostgreSQL จากเครื่อง host: `localhost:5434` (ค่าปริยาย)

Liquibase migration และข้อมูลตัวอย่างทำงานใน service `migrate` ก่อน `app` ส่วน frontend เปิดพอร์ต `3000` และเรียก backend ที่พอร์ต `8081`

หยุด container โดยเก็บข้อมูลฐานข้อมูลไว้:

```powershell
docker compose down
```

ข้อมูล PostgreSQL เก็บใน Docker volume ชื่อ `aomaoi_postgres_data` จึงยังอยู่หลังปิดและเปิด container ใหม่ หากต้องการลบฐานข้อมูลในเครื่องและเริ่มใหม่ทั้งหมด ใช้ `docker compose down -v` ซึ่งจะลบข้อมูลใน volume อย่างถาวร ตรวจให้แน่ใจก่อนสั่ง และอย่าใช้กับข้อมูลที่ต้องการเก็บไว้

## บัญชีตัวอย่างสำหรับเครื่องพัฒนา

Liquibase demo context สร้าง superadmin 1 บัญชี, admin 5 บัญชี, worker 20 บัญชี และฟาร์ม 5 แห่งในฐานข้อมูลใหม่:

| Role | Username | รหัสเริ่มต้น | หมายเหตุ |
|---|---|---|---|
| Superadmin | `superadmin` | `1234` | บัญชีตัวอย่างสำหรับ local; เปลี่ยนรหัสก่อนนำไปใช้งานจริง |
| Admin | `demo_admin01` ถึง `demo_admin05` | `Temp1234` | ระบบบังคับเปลี่ยนรหัสเมื่อเข้าสู่ระบบครั้งแรก |
| Worker | `demo_worker01` ถึง `demo_worker20` | `Temp1234` | ระบบบังคับเปลี่ยนรหัสเมื่อเข้าสู่ระบบครั้งแรก |

บัญชีและรหัสชุดนี้เป็น **ข้อมูลสาธิต** ไม่ควรนำไปใช้บนระบบจริง การเปลี่ยน `BOOTSTRAP_ADMIN_PASSWORD` ไม่ได้เปลี่ยนรหัส demo ที่ Liquibase สร้างให้

## รัน backend โดยไม่ใช้ Docker สำหรับแอป

ยังต้องมี PostgreSQL ที่ใช้งานได้ วิธีที่ง่ายคือให้ Compose เปิดเฉพาะฐานข้อมูล:

```powershell
docker compose up -d postgres
```

จากนั้นตั้งค่า environment variables ใน shell ให้ Spring Boot เชื่อมต่อฐานข้อมูล ตัวอย่าง PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5434/aomaoi_db"
$env:SPRING_DATASOURCE_USERNAME = "aomaoi_user"
$env:SPRING_DATASOURCE_PASSWORD = "aomaoi_password"
$env:JWT_SECRET = "replace-this-with-a-random-secret-at-least-32-bytes"
```

Spring Boot ไม่อ่านไฟล์ `.env` โดยอัตโนมัติ ต้อง export ค่าลง environment เอง (หรือใช้เครื่องมือจัดการ environment variables) แล้วเริ่มแอป:

```powershell
.\mvnw.cmd spring-boot:run
```

โดยปกติ Liquibase จะทำ migration ตอนเริ่มแอป หากต้องการสร้าง superadmin ด้วย application seeder ให้ตั้ง `APP_SEED_ENABLED=true`; `APP_SEED_DEMO_DATA=true` จะเปิดข้อมูลตัวอย่างของ seeder ด้วย ค่าเหล่านี้แยกจาก demo seed context ที่ Compose ใช้

## โครงสร้างโปรเจกต์

```text
src/main/java/com/aomaoi/backend/
├── config/       การตั้งค่า Spring Security, JWT, seeder และ E2E filter
├── controller/   จุดรับ HTTP request และส่ง HTTP response
├── dto/          รูปแบบข้อมูล request/response ที่ใช้รับส่งผ่าน API
├── entity/       Java entity ที่ map กับตาราง PostgreSQL ผ่าน JPA
├── exception/    การจัดการข้อผิดพลาดกลาง
├── repository/   การค้นหาและบันทึกข้อมูลด้วย Spring Data JPA
└── service/      กฎธุรกิจและการประสานงานระหว่าง repository

src/main/resources/
├── application.properties
└── db/changelog/  Liquibase master changelog และ SQL changesets

src/test/java/     unit และ integration tests
docs/              รายละเอียด migration และ shared E2E
k8s/               Kubernetes manifests และ Kustomize overlays
scripts/           สคริปต์ deploy และ smoke test สำหรับ Kubernetes
.github/workflows/ GitHub Actions CI/CD
Dockerfile         สร้าง backend image
docker-compose.yml เปิด PostgreSQL, migration, API และ frontend สำหรับ local
pom.xml            dependencies และการตั้งค่า Maven
```

การไหลของ request โดยทั่วไปคือ **Controller → Service → Repository → Entity/Database**: controller แปลง HTTP input/output, service ตรวจและใช้กฎธุรกิจ, repository ทำงานกับฐานข้อมูล และ entity เป็น mapping ของข้อมูลในตาราง ส่วน DTO ช่วยกำหนดรูปแบบข้อมูลที่ API รับหรือส่งโดยไม่จำเป็นต้องเปิดเผย entity ทุกกรณี

## Database และ migration

ฐานข้อมูลหลักคือ PostgreSQL โดย JPA/Hibernate map entity เข้ากับตาราง และ **Liquibase เป็นผู้จัดการการเปลี่ยน schema และ seed data** ไม่ควรแก้ schema ด้วยมือหรือแก้ changeset ที่เคยถูก apply แล้ว เพราะ checksum ของ Liquibase จะไม่ตรงกัน

Master changelog อยู่ที่ `src/main/resources/db/changelog/db.changelog-master.yaml` และเรียก changesets ตามลำดับ:

1. `001-initial-schema.sql` สร้างตารางและ constraint เริ่มต้น
2. `002-seed-demo-data.sql` เพิ่มบัญชีและข้อมูลตัวอย่างเมื่อใช้ context `demo`
3. `003-rename-demo-farms.sql` ปรับชื่อฟาร์มตัวอย่างโดยคง ID เดิม
4. `004-e2e-run-tracking.sql` เพิ่มโครงสร้างติดตามข้อมูลตามรอบ Cypress E2E

Compose เรียก Liquibase ด้วย demo context ใน service `migrate` และแอปกำหนด Hibernate `ddl-auto=validate` เพื่อเช็คว่า entity ตรงกับ schema หลัง migration

ชุด migration เริ่มต้นออกแบบสำหรับ **ฐานข้อมูลใหม่ที่ว่างเปล่า** ไม่ใช่การนำเข้าหรือแปลงประวัติฐานข้อมูลเก่าที่มีอยู่แล้ว Liquibase บันทึก changeset ที่รันแล้วไว้ใน `databasechangelog` และรันเฉพาะ changeset ใหม่ การเปลี่ยน `.env` หลังจาก PostgreSQL volume ถูกสร้างแล้วไม่ได้เปลี่ยน username/password ของฐานข้อมูลที่มีอยู่

รายละเอียดการเขียน migration, rollback และ migration integration test อยู่ใน [`docs/database-migrations.md`](docs/database-migrations.md)

## API หลัก

API ใช้ JSON และ base URL ปกติ `http://localhost:8081`. ยกเว้น login, health check และเส้นทางทดสอบที่กำหนดไว้ คำขอต้องส่ง access token ใน header:

```http
Authorization: Bearer <JWT>
Content-Type: application/json
```

| กลุ่ม | Endpoint | หน้าที่ |
|---|---|---|
| Auth | `POST /api/auth/login` | ตรวจ username/password และรับ JWT กับข้อมูลผู้ใช้ |
| Auth | `POST /api/auth/change-password` | เปลี่ยนรหัสผ่านของบัญชีที่ล็อกอิน |
| Dashboard | `GET /api/dashboard/overview` | รับตัวเลขภาพรวมระบบ |
| Farms | `GET /api/farms` | ดูรายการฟาร์ม |
| Farms | `POST /api/farms` | สร้างฟาร์ม (superadmin) |
| Farms | `PUT /api/farms/{id}` | แก้ไขฟาร์ม (superadmin) |
| Farms | `DELETE /api/farms/{id}` | ปิดใช้งานฟาร์ม (soft delete, superadmin) |
| Admins | `GET /api/admins`, `GET /api/admins/{id}` | ดูบัญชีผู้ดูแลฟาร์ม |
| Admins | `POST /api/admins`, `PUT /api/admins/{id}`, `DELETE /api/admins/{id}` | สร้าง/แก้ไข/ปิดใช้งานบัญชี admin (superadmin) |
| Workers | `GET /api/workers`, `GET /api/workers/{id}` | ดูข้อมูลคนงาน โดย admin ถูกจำกัดตามฟาร์มของตน |
| Workers | `POST /api/workers`, `PUT /api/workers/{id}`, `DELETE /api/workers/{id}` | จัดการบัญชีคนงานตามสิทธิ์ |
| Workers | `POST /api/workers/{id}/reset-password` | รีเซ็ตรหัสผ่านคนงานโดย admin หรือ superadmin |
| Work logs | `GET /api/work-logs` | ดูบันทึกงานตามสิทธิ์ของผู้ใช้ |
| Work logs | `POST /api/work-logs` | บันทึกงานและให้ backend คำนวณค่าจ้าง |
| Audit | `GET /api/superadmin/audit-logs` | ดูประวัติการใช้งาน (superadmin) |

ตัวอย่าง request สำหรับเข้าสู่ระบบ:

```json
{
  "username": "superadmin",
  "password": "1234"
}
```

ตัวอย่าง response เมื่อสำเร็จ (ค่าจริงของ token และข้อมูลผู้ใช้จะต่างกัน):

```json
{
  "success": true,
  "token": "<JWT>",
  "user": {
    "username": "superadmin",
    "role": "superadmin"
  }
}
```

ตัวอย่าง request บันทึกงานตัดอ้อย:

```json
{
  "type": "cutting",
  "workerId": 1,
  "date": "2026-10-20",
  "rows": 10,
  "waPerRow": 100
}
```

Response ของ work log มี ID, ประเภทงาน, วันที่, คนงาน และยอดที่คำนวณแล้ว ทั้งชื่อ field ที่จำเป็นและการคำนวณขึ้นกับประเภทงาน ดูรูปแบบ request เฉพาะแต่ละงานได้ใน `WorkLogController` และ `WorkLogRequestDTO`

## Authentication และสิทธิ์

Spring Security ทำงานแบบ stateless: server ไม่เก็บ session; เมื่อ login สำเร็จ backend ออก JWT ให้ frontend ส่งกลับมาใน `Authorization: Bearer ...` สำหรับคำขอที่ต้องยืนยันตัวตน `JwtAuthenticationFilter` ตรวจ token แล้วตั้งผู้ใช้และ role ให้ Spring Security ส่วน BCrypt ใช้ hash รหัสผ่านก่อนจัดเก็บ

สิทธิ์ตัวอย่างจากการตั้งค่า security:

- `superadmin` จัดการฟาร์มและบัญชี admin และดู audit log ได้
- `admin` จัดการคนงานและบันทึกงาน โดยข้อมูลที่ service คืนจะจำกัดตามฟาร์ม
- ผู้ใช้ทุก role ที่ล็อกอินแล้วเรียก API ที่ไม่ได้เปิดสาธารณะได้ แต่ service/controller ยังคงใช้ role และความสัมพันธ์กับฟาร์มกำหนดการเข้าถึงข้อมูล
- `POST /api/auth/login` เปิดให้เรียกโดยไม่ต้องมี token; password เปลี่ยนผ่าน endpoint ที่ต้องล็อกอิน

## ทดสอบ

รัน unit และ Spring tests ทั้งหมดด้วย Maven Wrapper:

```powershell
.\mvnw.cmd test
```

หรือบน Linux/macOS/Git Bash:

```bash
chmod +x mvnw
./mvnw test
```

Migration integration test ต้องใช้ PostgreSQL test database ที่แยกจากฐานข้อมูลจริง โดยตั้ง `AOMAOI_MIGRATION_TEST_URL`, `AOMAOI_MIGRATION_TEST_USER` และ `AOMAOI_MIGRATION_TEST_PASSWORD` ก่อนรัน:

```powershell
$env:AOMAOI_MIGRATION_TEST_URL = "jdbc:postgresql://localhost:5434/aomaoi_test"
$env:AOMAOI_MIGRATION_TEST_USER = "aomaoi_user"
$env:AOMAOI_MIGRATION_TEST_PASSWORD = "aomaoi_password"
.\mvnw.cmd -Dtest=PostgreSqlMigrationIntegrationTest test
```

ถ้าไม่ได้กำหนด test database การทดสอบ migration บางส่วนจะถูก skip โดยตั้งใจ อย่าชี้ตัวแปรนี้ไปยังฐานข้อมูล production

### Cypress E2E

Cypress อยู่ใน frontend repository และรันกับ stack หลักเดียวกัน ก่อนเริ่มให้ตั้ง `E2E_SUPPORT_KEY` เป็นค่าสุ่มอย่างน้อย 32 ตัวอักษรใน backend `.env` จากนั้น:

```powershell
# จาก aomaoi-backend: build และเปิด PostgreSQL, migration, API และ frontend
docker compose up --build -d

# จาก aomaoi-frontend: ติดตั้ง dependencies และรัน Cypress
cd ..\aomaoi-frontend
npm.cmd ci
npm.cmd run cypress:run
```

ข้อมูลที่ Cypress สร้างจะถูกติดตามด้วยรหัสรอบและลบเมื่อจบ spec; backend เก็บกวาดรอบที่หมดอายุหากการทดสอบหยุดกลางคัน อย่างไรก็ตามข้อมูลทดสอบจะแสดงในแอประหว่างที่เทสทำงาน จึงควรรันเฉพาะกับฐานข้อมูลที่ตั้งใจให้ใช้ทดสอบ อ่านข้อจำกัดและกลไกความปลอดภัยที่ [`docs/shared-e2e.md`](docs/shared-e2e.md)

## Kubernetes และ CI/CD

โฟลเดอร์ `k8s/` มี manifests สำหรับ namespace/config, PostgreSQL StatefulSet และ persistent volume, Liquibase migration Job, backend Deployment/Service และ Ingress ส่วน `scripts/k8s-deploy.sh` deploy ตามลำดับ และ `scripts/k8s-smoke.sh` ทดสอบ health, login และ API ผ่าน Ingress

GitHub Actions ใน `.github/workflows/build.yml` รันทดสอบและตรวจ manifests, build backend image, ทดสอบ deployment บน kind ชั่วคราวและ smoke test แล้ว deploy ไปยัง cluster จริงเมื่อมีการตั้งค่า `KUBE_CONFIG` และตรงเงื่อนไข workflow การตั้งค่า Kubernetes แบบละเอียดและ secrets ที่จำเป็นอยู่ใน [`k8s/README.md`](k8s/README.md)

## การตั้งค่าสำคัญ

| ตัวแปร | ใช้ทำอะไร |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT` | ชื่อ/บัญชีฐานข้อมูลและพอร์ต PostgreSQL บน host สำหรับ Compose |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | ค่าการเชื่อมต่อฐานข้อมูลของ Spring Boot เมื่อรันนอก Compose |
| `JWT_SECRET` | signing secret ของ JWT; ใช้ secret แบบสุ่มที่ยาวอย่างน้อย 32 bytes |
| `JWT_EXPIRATION` | อายุ JWT หน่วย millisecond; ค่าเริ่มต้น 86,400,000 (24 ชั่วโมง) |
| `E2E_SUPPORT_KEY` | กุญแจลับสำหรับเริ่มและ cleanup E2E run; ต้องสุ่มและยาวอย่างน้อย 32 ตัวอักษร |
| `FRONTEND_PORT` | พอร์ต frontend บน host; ค่าเริ่มต้น 3000 |
| `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE` | ปรับขนาด Hikari connection pool |

ดูค่าตัวอย่างและคำอธิบายเพิ่มเติมใน [`.env.example`](.env.example) ไฟล์ `.env` จริงถูก ignore โดย Git และต้องไม่ commit

## ข้อควรทราบ

- Migration ปัจจุบันเริ่มจากฐานข้อมูลว่าง ไม่ได้ migrate/import ฐานข้อมูลเก่าหรือข้อมูลจาก Supabase โดยอัตโนมัติ
- อย่าแก้ changeset ที่ apply ไปแล้ว ให้สร้าง changeset ลำดับใหม่และเพิ่มเข้า master changelog
- ข้อมูล demo และรหัสตัวอย่างมีไว้สำหรับ development เท่านั้น
- `docker compose down` เก็บข้อมูลฐานข้อมูล; `docker compose down -v` ลบ Docker volume และข้อมูลถาวรในเครื่อง
- Cypress ใช้ฐานข้อมูลแอปเดียวกันและข้อมูลทดสอบอาจมองเห็นได้ชั่วคราว อย่ารันกับระบบหรือฐานข้อมูล production
- อย่าเผยแพร่ `JWT_SECRET`, `E2E_SUPPORT_KEY`, รหัสฐานข้อมูล, kubeconfig หรือ secret ของระบบจริงใน source control หรือ screenshot
