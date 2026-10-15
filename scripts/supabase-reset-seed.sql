-- MANUAL, DESTRUCTIVE: Run once in the Supabase SQL Editor only when you intend
-- to erase all Aomaoi application records in public and replace them with demo data.
-- Liquibase history is deliberately preserved. All seeded accounts use password 1234.

BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
SET LOCAL search_path TO public, extensions;

DO $reset$
DECLARE
    table_to_clear TEXT;
BEGIN
    FOREACH table_to_clear IN ARRAY ARRAY[
        'audit_logs', 'work_logs', 'admin_profile', 'worker', 'farm', 'users',
        'workers', 'farms'
    ] LOOP
        IF to_regclass(format('public.%I', table_to_clear)) IS NOT NULL THEN
            EXECUTE format('TRUNCATE TABLE public.%I RESTART IDENTITY CASCADE', table_to_clear);
        END IF;
    END LOOP;
END
$reset$;

INSERT INTO public.farm (name, location, worker_count, admin_count, status, joined_date)
SELECT farm_data.name,
       farm_data.location,
       4,
       1,
       'active',
       CURRENT_DATE
FROM (VALUES
    (1, 'ฟาร์มเพื่อพ่อ', 'จังหวัดชลบุรี'),
    (2, 'ไร่สุขใจ', 'จังหวัดเชียงใหม่'),
    (3, 'สวนแสงตะวัน', 'จังหวัดระยอง'),
    (4, 'ฟาร์มบ้านนา', 'จังหวัดนครราชสีมา'),
    (5, 'เกษตรพอเพียง', 'จังหวัดสุพรรณบุรี')
) AS farm_data(farm_number, name, location)
ORDER BY farm_number;

WITH credential AS (
    SELECT crypt('1234', gen_salt('bf', 10)) AS password_hash
),
seed_accounts AS (
    SELECT 'superadmin'::TEXT AS username, 'superadmin'::TEXT AS role
    UNION ALL
    SELECT 'admin' || admin_number, 'admin'
    FROM generate_series(1, 5) AS series(admin_number)
    UNION ALL
    SELECT 'worker' || worker_number, 'worker'
    FROM generate_series(1, 20) AS series(worker_number)
)
INSERT INTO public.users (username, password, role, is_first_login)
SELECT seed_accounts.username, credential.password_hash, seed_accounts.role, FALSE
FROM seed_accounts CROSS JOIN credential;

INSERT INTO public.admin_profile (full_name, phone, farm_id, status, joined_date, user_id)
SELECT admin_data.full_name,
       '090000' || lpad(admin_number::TEXT, 4, '0'),
       farm.id,
       'active',
       CURRENT_DATE,
       account.id
FROM generate_series(1, 5) AS series(admin_number)
JOIN public.users AS account ON account.username = 'admin' || admin_number
JOIN public.farm AS farm ON farm.id = admin_number
JOIN (VALUES
    (1, 'สมศักดิ์ รักฟาร์ม'),
    (2, 'มาลี ใจดี'),
    (3, 'ประเสริฐ เกษตรสุข'),
    (4, 'วิไลพร บ้านนา'),
    (5, 'ธนากร พอเพียง')
) AS admin_data(admin_number, full_name) USING (admin_number);

INSERT INTO public.worker (full_name, nickname, phone, status, farm_id, joined_date, user_id)
SELECT worker_data.full_name,
       'คนงาน ' || lpad(worker_number::TEXT, 2, '0'),
       '080000' || lpad(worker_number::TEXT, 4, '0'),
       'active',
       farm.id,
       CURRENT_DATE,
       account.id
FROM generate_series(1, 20) AS series(worker_number)
JOIN public.users AS account ON account.username = 'worker' || worker_number
JOIN public.farm AS farm ON farm.id = (((worker_number - 1) % 5) + 1)
JOIN (VALUES
    (1, 'สมชาย ใจกล้า'), (2, 'สุดา ขยันงาน'), (3, 'ประยุทธ รักผัก'), (4, 'นิดา แสงทอง'),
    (5, 'คำปัน ใจเย็น'), (6, 'ดาวเรือง เมืองเหนือ'), (7, 'บุญมี ขยันดี'), (8, 'อรุณี รักสวน'),
    (9, 'วิชัย ชาวสวน'), (10, 'พรทิพย์ ยิ้มแย้ม'), (11, 'เอกชัย ผลไม้ดี'), (12, 'รัชนี แสงแดด'),
    (13, 'สมบัติ ไร่ทอง'), (14, 'กมลวรรณ บ้านนา'), (15, 'บุญช่วย พืชงาม'), (16, 'อำนวย ใจรัก'),
    (17, 'เกษม พอเพียง'), (18, 'นภาพร ร่มเย็น'), (19, 'ชัยณรงค์ ดินดี'), (20, 'สุภาพร ผักหวาน')
) AS worker_data(worker_number, full_name) USING (worker_number);

-- Five logs for each of the four task types supported by WorkLogService:
-- cutting, planting, watering, and spraying. Keep type-specific fields and
-- totals consistent with the backend wage formulas.
WITH sample_logs AS (
    SELECT worker_number,
           CASE ((worker_number - 1) % 4)
               WHEN 0 THEN 'cutting'
               WHEN 1 THEN 'planting'
               WHEN 2 THEN 'watering'
               ELSE 'spraying'
           END AS type,
           CURRENT_DATE - (20 - worker_number) AS work_date,
           10 AS rows,
           100 AS wa_per_row,
           15 AS furrows,
           20 AS wa_per_furrow,
           3 AS days,
           350.00::NUMERIC(38,2) AS daily_rate,
           5 AS tanks
    FROM generate_series(1, 20) AS series(worker_number)
)
INSERT INTO public.work_logs
    (worker_id, type, start_date, end_date, work_date, rows, wa_per_row,
     furrows, wa_per_furrow, days, daily_rate, tanks, total)
SELECT worker.id,
       sample_logs.type,
       sample_logs.work_date,
       CASE WHEN sample_logs.type = 'watering'
            THEN sample_logs.work_date + 2
            ELSE sample_logs.work_date
       END,
       sample_logs.work_date,
       CASE WHEN sample_logs.type = 'cutting' THEN sample_logs.rows END,
       CASE WHEN sample_logs.type = 'cutting' THEN sample_logs.wa_per_row END,
       CASE WHEN sample_logs.type = 'planting' THEN sample_logs.furrows END,
       CASE WHEN sample_logs.type = 'planting' THEN sample_logs.wa_per_furrow END,
       CASE WHEN sample_logs.type = 'watering' THEN sample_logs.days END,
       CASE WHEN sample_logs.type = 'watering' THEN sample_logs.daily_rate END,
       CASE WHEN sample_logs.type = 'spraying' THEN sample_logs.tanks END,
       CASE sample_logs.type
           WHEN 'cutting' THEN (sample_logs.rows * sample_logs.wa_per_row * 2)::NUMERIC(38,2)
           WHEN 'planting' THEN (sample_logs.furrows * sample_logs.wa_per_furrow * 2.5)::NUMERIC(38,2)
           WHEN 'watering' THEN (sample_logs.days * sample_logs.daily_rate)::NUMERIC(38,2)
           WHEN 'spraying' THEN (sample_logs.tanks * 150)::NUMERIC(38,2)
       END
FROM sample_logs
JOIN public.users AS account ON account.username = 'worker' || sample_logs.worker_number
JOIN public.worker AS worker ON worker.user_id = account.id;

DO $verify$
BEGIN
    IF (SELECT count(*) FROM public.users) <> 26
       OR (SELECT count(*) FROM public.farm) <> 5
       OR (SELECT count(*) FROM public.admin_profile) <> 5
       OR (SELECT count(*) FROM public.worker) <> 20
       OR (SELECT count(*) FROM public.work_logs) <> 20
       OR (SELECT count(DISTINCT type) FROM public.work_logs) <> 4
       OR EXISTS (
           SELECT 1 FROM public.work_logs
           WHERE type NOT IN ('cutting', 'planting', 'watering', 'spraying')
       ) THEN
        RAISE EXCEPTION 'Seed verification failed; rolling back reset and seed.';
    END IF;
END
$verify$;

COMMIT;

-- Expected application row counts: users=26, farm=5, admin_profile=5, worker=20,
-- work_logs=20, audit_logs=0. Run this read-only verification separately if desired:
-- SELECT 'users' AS table_name, count(*) FROM public.users
-- UNION ALL SELECT 'farm', count(*) FROM public.farm
-- UNION ALL SELECT 'admin_profile', count(*) FROM public.admin_profile
-- UNION ALL SELECT 'worker', count(*) FROM public.worker
-- UNION ALL SELECT 'work_logs', count(*) FROM public.work_logs
-- UNION ALL SELECT 'audit_logs', count(*) FROM public.audit_logs;
