# อีเมลสรุปประจำวันถึง Admin

ทุกวัน 18:00 (เวลาไทย) ระบบส่งอีเมลถึง Admin ของแต่ละฟาร์ม สรุป:

- **คนงานเด่นของวัน** = คนที่ค่าแรงรวมของงานที่บันทึกในวันนั้นสูงสุด (เท่ากันดูจำนวนงาน แล้วเรียงชื่อ)
- **ขาด / ลา (พร้อมหมายเหตุ) / จำนวนที่มา** = มาจากข้อมูลเช็กชื่อ (ดู `docs/attendance.md`) ไม่ได้เดาจากบันทึกงานอีกแล้ว
- ค่าแรงรวมของวัน

ข้อจำกัดที่ต้องรู้
- ค่าแรง (`total`) เก็บต่อรายการ ไม่ได้เก็บต่อวัน งานรดน้ำที่คร่อมหลายวันจึงถูกนับค่าแรงทั้งก้อนในวันเริ่มต้น
- **วันหยุด**: ถ้าฟาร์มไม่มีแถวเช็กชื่อและไม่มีบันทึกงานเลยในวันนั้น จะไม่ส่งอีเมลของวันนั้น
- อีเมลส่งหลังปิดวัน (`ATTENDANCE_CLOSE_DAY_ENABLED=true` ให้ระบบเติมแถวให้คนที่ยังไม่ได้เช็กก่อนส่งในรอบเดียวกัน) ถ้าไม่เปิด จะเห็นเฉพาะคนที่ Admin เช็กเอง
- ตอนส่งแสดงเฉพาะงานที่บันทึกถึงเวลานั้น (บันทึกย้อนหลังทีหลังไม่ถูกส่งซ้ำ)

## เปิดใช้งาน

1. ตั้งค่า env (ดู `.env.example`): `DAILY_SUMMARY_ENABLED=true`, `MAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`
2. ใส่อีเมลให้ Admin (หน้า Admins Management หรือ field `email` ของ API `/api/admins`) Admin ที่ไม่มีอีเมล, ยังไม่ active, หรือไม่มีฟาร์ม จะไม่ได้รับ
3. ไม่ตั้ง `SMTP_HOST`/`MAIL_FROM` = ข้ามการส่ง (นับ metric `skipped`) ไม่ทำให้แอปพัง

## ไม่ส่งซ้ำ (หลาย replica / restart)

ก่อนส่ง ระบบ `INSERT ... ON CONFLICT DO NOTHING` ลง `daily_summary_log` (unique `admin_id + summary_date`) ใครแทรกแถวได้คนนั้นส่ง ส่งไม่สำเร็จจะเป็น `FAILED` และลองใหม่ทุก 15 นาที (18:15 ... 20:45) สูงสุด 3 ครั้ง แถวค้างสถานะ `SENDING` เกิน 30 นาทีถือว่าส่งไม่สำเร็จ

## ทดสอบในเครื่องด้วย Mailpit

```bash
# ใน .env: DAILY_SUMMARY_ENABLED=true, MAIL_FROM=no-reply@aomaoi.local, SMTP_HOST=mailpit, SMTP_PORT=1025
docker compose up --build -d
# สั่งส่งให้ Admin คนเดียวทันที (ต้องมี E2E_SUPPORT_KEY) แล้วเปิด http://localhost:8025
curl -X POST "http://localhost:8081/api/test-support/daily-summary/run?adminId=<id>&date=2026-11-12" -H "X-E2E-Support-Key: <key>"
```

เรียกซ้ำวันเดิมจะได้ `ALREADY_HANDLED` (ไม่ส่งซ้ำ) endpoint นี้ส่งให้ admin คนเดียวเท่านั้น กันเผลอส่งหาทุกคนในฐานข้อมูลที่ใช้ร่วมกัน

## Metrics และ audit

- Counter `aomaoi.daily_summary.emails{outcome=sent|failed|skipped}` (Prometheus)
- Audit log `DAILY_SUMMARY_SENT` (performedBy = system)
