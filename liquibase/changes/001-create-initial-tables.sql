--liquibase formatted sql

--changeset aomaoi:001-create-users-table
--comment: สร้างตาราง users สำหรับระบบ Authentication
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'worker',
    is_first_login BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--rollback DROP TABLE IF EXISTS users;

--changeset aomaoi:002-create-farms-table
--comment: สร้างตาราง farms สำหรับข้อมูลฟาร์ม
CREATE TABLE IF NOT EXISTS farms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    status VARCHAR(50) DEFAULT 'active',
    worker_count INT DEFAULT 0,
    admin_count INT DEFAULT 0,
    monthly_wages NUMERIC(12, 2) DEFAULT 0.0,
    total_wages NUMERIC(12, 2) DEFAULT 0.0,
    joined_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--rollback DROP TABLE IF EXISTS farms;

--changeset aomaoi:003-create-workers-table
--comment: สร้างตาราง workers สำหรับข้อมูลคนงาน
CREATE TABLE IF NOT EXISTS workers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    farm_id BIGINT REFERENCES farms(id) ON DELETE SET NULL,
    full_name VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(50) DEFAULT 'active',
    joined_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

--rollback DROP TABLE IF EXISTS workers;
