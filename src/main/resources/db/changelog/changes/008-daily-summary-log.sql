--liquibase formatted sql

--changeset aomaoi:008-daily-summary-log dbms:postgresql
--comment: One row per admin per day. The unique key lets several app replicas race safely: only the one that inserts the row sends the e-mail.
CREATE TABLE daily_summary_log (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL REFERENCES admin_profile (id) ON DELETE CASCADE,
    summary_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 1,
    sent_at TIMESTAMPTZ,
    error VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_summary_admin_date UNIQUE (admin_id, summary_date),
    CONSTRAINT ck_daily_summary_status CHECK (status IN ('SENDING', 'SENT', 'FAILED'))
);

--rollback DROP TABLE daily_summary_log;
