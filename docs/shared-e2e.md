# E2E บนระบบหลักร่วมกัน

Cypress ใช้ frontend พอร์ต 3000, backend พอร์ต 8081 และ PostgreSQL ของแอปหลัก
ไม่มีบริการ `e2e-postgres`, `e2e-backend`, `e2e-frontend` อีกต่อไป
Migration test ยังใช้ฐานข้อมูลทดสอบเฉพาะได้ตามเดิม เพราะมีเคส rollback schema

## ตั้งค่าครั้งแรก

1. สร้างค่า `E2E_SUPPORT_KEY` แบบสุ่มอย่างน้อย 32 ตัวอักษรใน `aomaoi-backend/.env`
   ค่านี้อนุญาตให้สร้างบัญชีทดสอบสิทธิ์ superadmin และล้างข้อมูลทดสอบ จึงต้องเก็บเป็นความลับ
   ค่าเปล่าปิดการสร้างรอบทดสอบโดยไม่ปิดการใช้งานแอป
2. ใช้ backend และ frontend เวอร์ชันที่รองรับการติดตามรอบทดสอบทั้งคู่
3. รันจาก backend:

```powershell
docker compose up --build -d --remove-orphans
docker compose logs --tail 100 migrate app
Invoke-RestMethod http://localhost:8081/api/test-support/e2e-readiness
```

ผลควรมี `safeForE2e: true` และ `mode: shared-tracked`
`--remove-orphans` นำ container บริการเก่าที่ไม่อยู่ใน Compose ออก โดยไม่ลบ volume ฐานข้อมูลหลัก
ไม่ต้องใช้ `down -v` และไม่ต้องเปิด dev server เพิ่ม
หาก migration service เดิมไม่รันหลังอัปเดตโค้ด ให้ `docker compose run --rm migrate` แล้ว recreate app

```powershell
cd ..\aomaoi-frontend
npm.cmd ci
npm.cmd run cypress:run
# หรือเปิดหน้าต่าง Cypress
npm.cmd run cypress:open
```

Node ของ Cypress อ่าน key จาก environment หรือ `../aomaoi-backend/.env`
ถ้าใช้ Compose ฝั่ง frontend ซึ่งดึง backend image ให้ตั้ง key ค่าเดียวกันใน environment/.env ฝั่งนั้นด้วย
ถ้ารันกับ URL อื่น ให้ตั้ง `--config baseUrl=... --env apiUrl=...` และตรวจ build ของ frontend ให้ชี้ API เดียวกัน

## ข้อมูลถูกติดตามและลบอย่างไร

- เริ่มแต่ละ spec ผ่าน API ที่ตรวจ management key; สร้าง `e2e_runs` และ superadmin ชั่วคราว
- Cypress ส่ง `X-E2E-Run` และ token เฉพาะรอบทั้งคำขอจาก browser และ `cy.request`
  management key อยู่เฉพาะ Node ไม่ส่งเข้า browser
- backend ตรวจ token และเปิด transaction สำหรับคำขอทดสอบ พร้อมกำหนด PostgreSQL transaction-local setting
- trigger ติด `e2e_run_id` ให้ users, farm, worker, admin_profile, work_logs และ audit_logs ที่ถูกสร้าง
  จึงติดตามได้แม้เปลี่ยนชื่อหรือ soft delete ไปแล้ว
- trigger ปฏิเสธการแก้/ลบข้อมูลคนละรอบหรือข้อมูลจริง และการผูกโปรไฟล์/ประวัติงานข้ามชุดข้อมูล
- คำขอปกติใช้เส้นทางเดิม แต่แก้ข้อมูลที่ยังเป็นของรอบทดสอบไม่ได้
- เมื่อจบ spec ทั้งผ่านและไม่ผ่าน Cypress ขอ cleanup; backend ลบตามลำดับ foreign key ใน transaction
  แล้วตรวจว่าทุกตารางเหลือข้อมูลของรอบนั้นเป็นศูนย์ จึงลบรอบทดสอบ
- `after:spec` และ `after:run` เป็น fallback; cleanup ซ้ำได้โดยไม่ลบข้อมูลอื่น
- Node ส่ง heartbeat ทุกนาที หากหยุดกลางทางและไม่มี heartbeat/คำขอเกิน 15 นาที
  scheduler ฝั่ง backend เก็บกวาดในรอบถัดไป (ตรวจทุกนาที)
  ถ้า backend ปิดอยู่ จะเก็บกวาดหลัง backend กลับมาทำงาน
- row lock ป้องกัน cleanup แข่งกับคำขอของรอบที่กำลังทำงาน

## ขอบเขตและข้อจำกัด

ข้อมูลทดสอบปรากฏในหน้ารายการและยอดรวมชั่วคราว ผู้ใช้ไม่ควรพยายามแก้รายการเหล่านั้น
เทสต้องสร้าง fixture ของตัวเอง ห้ามคาดว่าจำนวนฟาร์ม/คนงานทั้งระบบเริ่มจากศูนย์
การทดสอบยังเรียก API และฐานข้อมูลจริง ไม่มีการ mock response ของแอป

การล้างคืนหมายถึงลบแถวข้อมูลทดสอบ ไม่ได้ย้อนค่า identity sequence, PostgreSQL WAL,
server log หรือผลข้างเคียงภายนอก เช่น การส่งอีเมล หากเพิ่มฟีเจอร์เหล่านั้นต้องออกแบบ cleanup เพิ่ม
ข้อมูล E2E เก่าที่สร้างก่อน migration 004 ไม่มีรหัสรอบ จึงไม่ถูกลบด้วยระบบนี้อัตโนมัติ

อย่าลบแถวใน `e2e_runs` โดยตรง เพราะเป็นเจ้าของข้อมูลที่ต้องลบตามลำดับ
การล้างรอบที่รู้ ID ใช้ `DELETE /api/test-support/runs/{id}` พร้อม management key
อย่าฝัง key ใน URL, source code, screenshot หรือ Git

## CI

Frontend workflow เปิด Compose ชุดหลักบน GitHub runner แล้วรัน Cypress กับ 3000/8081
ฐานข้อมูลบน runner เป็นของงานนั้น ไม่ได้เชื่อมต่อฐานข้อมูลของเครื่องทีมโดยอัตโนมัติ
ต้องเผยแพร่ backend image ที่มี migration 004 และ lifecycle API ก่อน pipeline frontend ใช้งานได้
Smoke test ของ Kubernetes และ migration test มีหน้าที่แยกจาก Cypress และยังคงอยู่
