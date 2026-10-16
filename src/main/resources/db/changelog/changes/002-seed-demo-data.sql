--liquibase formatted sql

--changeset aomaoi:002-seed-demo-data dbms:postgresql context:demo
--comment: Seed one superadmin, five admins, twenty workers, and five demo farms.

-- superadmin / 1234 is retained for Cypress. Other demo accounts use Temp1234.
-- Admins and workers must change their password on first login.
INSERT INTO users (username, password, role, is_first_login)
VALUES
    ('superadmin', '$2b$10$omX//hZc1IoDxvpSbGztIOXNdl1qaRwQ.rPXUJs.wAam1Mmdo.9xa', 'superadmin', FALSE),
    ('demo_admin01', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'admin', TRUE),
    ('demo_admin02', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'admin', TRUE),
    ('demo_admin03', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'admin', TRUE),
    ('demo_admin04', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'admin', TRUE),
    ('demo_admin05', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'admin', TRUE),
    ('demo_worker01', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker02', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker03', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker04', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker05', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker06', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker07', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker08', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker09', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker10', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker11', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker12', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker13', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker14', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker15', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker16', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker17', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker18', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker19', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE),
    ('demo_worker20', '$2b$10$ziwJznpKDQ/EJ9eT4z9BquH7H9HNcpPN1fEFHuh7qlPUST6iLoUWe', 'worker', TRUE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO farm (name, location, status, worker_count, admin_count, monthly_wages, total_wages, joined_date)
SELECT seed.name, seed.location, 'active', 4, 1, 0, 0, CURRENT_DATE
FROM (VALUES
    ('ฟาร์มเพื่อพ่อ (ตัวอย่าง)', 'ชลบุรี'),
    ('ไร่สุขใจ (ตัวอย่าง)', 'นครราชสีมา'),
    ('ไร่แสงทอง (ตัวอย่าง)', 'กาญจนบุรี'),
    ('ฟาร์มอ้อยบ้านสวน (ตัวอย่าง)', 'สุพรรณบุรี'),
    ('ไร่พอเพียง (ตัวอย่าง)', 'ขอนแก่น')
) AS seed(name, location)
WHERE NOT EXISTS (SELECT 1 FROM farm existing WHERE existing.name = seed.name);

INSERT INTO admin_profile (full_name, phone, farm_id, status, joined_date, user_id)
SELECT seed.full_name, seed.phone, farm.id, 'pending', CURRENT_DATE, users.id
FROM (VALUES
    ('demo_admin01', 'สมศักดิ์ รักฟาร์ม', '0800000001', 'ฟาร์มเพื่อพ่อ (ตัวอย่าง)'),
    ('demo_admin02', 'สมใจ ใจดี', '0800000002', 'ไร่สุขใจ (ตัวอย่าง)'),
    ('demo_admin03', 'มานะ ขยันงาน', '0800000003', 'ไร่แสงทอง (ตัวอย่าง)'),
    ('demo_admin04', 'สุดา รักอ้อย', '0800000004', 'ฟาร์มอ้อยบ้านสวน (ตัวอย่าง)'),
    ('demo_admin05', 'ประเสริฐ พอเพียง', '0800000005', 'ไร่พอเพียง (ตัวอย่าง)')
) AS seed(username, full_name, phone, farm_name)
JOIN users ON users.username = seed.username
JOIN LATERAL (
    SELECT id FROM farm WHERE name = seed.farm_name ORDER BY id LIMIT 1
) farm ON TRUE
WHERE users.role = 'admin'
  AND NOT EXISTS (SELECT 1 FROM admin_profile existing WHERE existing.user_id = users.id)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO worker (full_name, nickname, phone, status, farm_id, joined_date, user_id)
SELECT seed.full_name, seed.nickname, seed.phone, 'pending', farm.id, CURRENT_DATE, users.id
FROM (VALUES
    ('demo_worker01', 'สมชาย ใจกล้า', 'ชาย', '0800000101', 'ฟาร์มเพื่อพ่อ (ตัวอย่าง)'),
    ('demo_worker02', 'สมหญิง ขยัน', 'หญิง', '0800000102', 'ฟาร์มเพื่อพ่อ (ตัวอย่าง)'),
    ('demo_worker03', 'วิชัย รักงาน', 'ชัย', '0800000103', 'ฟาร์มเพื่อพ่อ (ตัวอย่าง)'),
    ('demo_worker04', 'มาลี ใจดี', 'ลี', '0800000104', 'ฟาร์มเพื่อพ่อ (ตัวอย่าง)'),
    ('demo_worker05', 'ประยุทธ แข็งแรง', 'ยุทธ', '0800000105', 'ไร่สุขใจ (ตัวอย่าง)'),
    ('demo_worker06', 'วาสนา อดทน', 'นา', '0800000106', 'ไร่สุขใจ (ตัวอย่าง)'),
    ('demo_worker07', 'สมพร ตั้งใจ', 'พร', '0800000107', 'ไร่สุขใจ (ตัวอย่าง)'),
    ('demo_worker08', 'อำนาจ ขยัน', 'นาจ', '0800000108', 'ไร่สุขใจ (ตัวอย่าง)'),
    ('demo_worker09', 'บุญมี มีสุข', 'บุญ', '0800000109', 'ไร่แสงทอง (ตัวอย่าง)'),
    ('demo_worker10', 'ดวงใจ ใจงาม', 'ดวง', '0800000110', 'ไร่แสงทอง (ตัวอย่าง)'),
    ('demo_worker11', 'กำพล มุ่งมั่น', 'พล', '0800000111', 'ไร่แสงทอง (ตัวอย่าง)'),
    ('demo_worker12', 'ราตรี ขยันดี', 'รี', '0800000112', 'ไร่แสงทอง (ตัวอย่าง)'),
    ('demo_worker13', 'ชูชาติ รักไร่', 'ชาติ', '0800000113', 'ฟาร์มอ้อยบ้านสวน (ตัวอย่าง)'),
    ('demo_worker14', 'พิมพ์ใจ สุขสันต์', 'พิม', '0800000114', 'ฟาร์มอ้อยบ้านสวน (ตัวอย่าง)'),
    ('demo_worker15', 'สุชาติ ใจเย็น', 'สุ', '0800000115', 'ฟาร์มอ้อยบ้านสวน (ตัวอย่าง)'),
    ('demo_worker16', 'อรทัย ขยันงาน', 'อร', '0800000116', 'ฟาร์มอ้อยบ้านสวน (ตัวอย่าง)'),
    ('demo_worker17', 'สมบัติ มีเพียร', 'บัติ', '0800000117', 'ไร่พอเพียง (ตัวอย่าง)'),
    ('demo_worker18', 'นันทนา ใจดี', 'นา', '0800000118', 'ไร่พอเพียง (ตัวอย่าง)'),
    ('demo_worker19', 'เอกชัย รักอ้อย', 'เอก', '0800000119', 'ไร่พอเพียง (ตัวอย่าง)'),
    ('demo_worker20', 'นภา ตั้งใจ', 'ภา', '0800000120', 'ไร่พอเพียง (ตัวอย่าง)')
) AS seed(username, full_name, nickname, phone, farm_name)
JOIN users ON users.username = seed.username
JOIN LATERAL (
    SELECT id FROM farm WHERE name = seed.farm_name ORDER BY id LIMIT 1
) farm ON TRUE
WHERE users.role = 'worker'
  AND NOT EXISTS (SELECT 1 FROM worker existing WHERE existing.user_id = users.id)
ON CONFLICT (user_id) DO NOTHING;

--rollback DELETE FROM work_logs WHERE worker_id IN (SELECT id FROM worker WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo_worker%'));
--rollback DELETE FROM worker WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo_worker%');
--rollback DELETE FROM admin_profile WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo_admin%');
--rollback DELETE FROM users WHERE username LIKE 'demo_admin%' OR username LIKE 'demo_worker%';
--rollback DELETE FROM users WHERE username = 'superadmin' AND password = '$2b$10$omX//hZc1IoDxvpSbGztIOXNdl1qaRwQ.rPXUJs.wAam1Mmdo.9xa' AND is_first_login = FALSE;
--rollback DELETE FROM farm WHERE name IN ('ฟาร์มเพื่อพ่อ (ตัวอย่าง)', 'ไร่สุขใจ (ตัวอย่าง)', 'ไร่แสงทอง (ตัวอย่าง)', 'ฟาร์มอ้อยบ้านสวน (ตัวอย่าง)', 'ไร่พอเพียง (ตัวอย่าง)');
