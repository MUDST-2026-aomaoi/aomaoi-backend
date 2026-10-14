--liquibase formatted sql

--changeset aomaoi:001-create-users-table
--comment: สร้างตาราง users สำหรับระบบ Authentication
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'worker',
    is_first_login BOOLEAN NOT NULL DEFAULT TRUE
);
--rollback DROP TABLE IF EXISTS users CASCADE;

--changeset aomaoi:002-create-farm-table
--comment: สร้างตาราง farm สำหรับข้อมูลฟาร์ม
CREATE TABLE IF NOT EXISTS farm (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    location VARCHAR(255),
    image TEXT,
    worker_count INT DEFAULT 0,
    admin_count INT DEFAULT 0,
    monthly_wages NUMERIC(12, 2) DEFAULT 0.0,
    total_wages NUMERIC(12, 2) DEFAULT 0.0,
    status VARCHAR(50) DEFAULT 'active',
    joined_date DATE
);
--rollback DROP TABLE IF EXISTS farm CASCADE;

--changeset aomaoi:003-create-worker-table
--comment: สร้างตาราง worker สำหรับข้อมูลคนงาน
CREATE TABLE IF NOT EXISTS worker (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255),
    nickname VARCHAR(100),
    phone VARCHAR(20),
    avatar TEXT,
    status VARCHAR(50) DEFAULT 'active',
    farm_id VARCHAR(255),
    joined_date DATE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL
);
--rollback DROP TABLE IF EXISTS worker CASCADE;

--changeset aomaoi:004-create-admin-profile-table
--comment: สร้างตาราง admin_profile สำหรับโปรไฟล์ผู้ดูแล
CREATE TABLE IF NOT EXISTS admin_profile (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255),
    phone VARCHAR(255),
    farm_id VARCHAR(255),
    avatar TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    joined_date DATE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL
);
--rollback DROP TABLE IF EXISTS admin_profile CASCADE;

--changeset aomaoi:005-create-audit-logs-table
--comment: สร้างตาราง audit_logs สำหรับประวัติการทำงาน
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    target_user VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    details TEXT
);
--rollback DROP TABLE IF EXISTS audit_logs CASCADE;

--changeset aomaoi:006-create-work-logs-table
--comment: สร้างตาราง work_logs สำหรับบันทึกผลงานคนงาน
CREATE TABLE IF NOT EXISTS work_logs (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT NOT NULL REFERENCES worker(id) ON DELETE CASCADE,
    type VARCHAR(255) NOT NULL,
    work_date DATE NOT NULL,
    rows INT,
    wa_per_row INT,
    furrows INT,
    wa_per_furrow INT,
    days INT,
    daily_rate NUMERIC(38, 2),
    tanks INT,
    total NUMERIC(38, 2) NOT NULL,
    start_date DATE,
    end_date DATE
);
--rollback DROP TABLE IF EXISTS work_logs CASCADE;

--changeset aomaoi:007-create-legacy-tables
--comment: สร้างตาราง farms และ workers สำรองเพื่อความเข้ากันได้กับ pg_dump sequence
CREATE TABLE IF NOT EXISTS farms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS workers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255)
);
--rollback DROP TABLE IF EXISTS farms, workers CASCADE;
