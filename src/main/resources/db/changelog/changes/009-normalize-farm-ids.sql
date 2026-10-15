--liquibase formatted sql

--changeset aomaoi:009-normalize-farm-ids splitStatements:false
--comment: Preserve farm IDs while converting references to BIGINT and enforcing relationships.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM worker WHERE farm_id IS NOT NULL AND btrim(farm_id::text) !~ '^[0-9]+$')
       OR EXISTS (SELECT 1 FROM admin_profile WHERE farm_id IS NOT NULL AND btrim(farm_id::text) !~ '^[0-9]+$') THEN
        RAISE EXCEPTION 'Farm ID migration blocked: non-numeric farm_id in worker or admin_profile. Correct the source records before retrying.';
    END IF;
END $$;

ALTER TABLE worker ALTER COLUMN farm_id TYPE BIGINT USING btrim(farm_id::text)::bigint;
ALTER TABLE admin_profile ALTER COLUMN farm_id TYPE BIGINT USING btrim(farm_id::text)::bigint;

-- Deferred checks allow related farm records to be inserted within one transaction.
ALTER TABLE worker ADD CONSTRAINT fk_worker_farm
    FOREIGN KEY (farm_id) REFERENCES farm(id) ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE admin_profile ADD CONSTRAINT fk_admin_profile_farm
    FOREIGN KEY (farm_id) REFERENCES farm(id) ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;

CREATE INDEX idx_worker_farm_id ON worker(farm_id);
CREATE INDEX idx_admin_profile_farm_id ON admin_profile(farm_id);
CREATE INDEX idx_work_logs_worker_date ON work_logs(worker_id, work_date DESC, id DESC);

--rollback DROP INDEX idx_work_logs_worker_date;
--rollback DROP INDEX idx_admin_profile_farm_id;
--rollback DROP INDEX idx_worker_farm_id;
--rollback ALTER TABLE admin_profile DROP CONSTRAINT fk_admin_profile_farm;
--rollback ALTER TABLE worker DROP CONSTRAINT fk_worker_farm;
--rollback ALTER TABLE admin_profile ALTER COLUMN farm_id TYPE VARCHAR(255) USING farm_id::text;
--rollback ALTER TABLE worker ALTER COLUMN farm_id TYPE VARCHAR(255) USING farm_id::text;
