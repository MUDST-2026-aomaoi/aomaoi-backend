--liquibase formatted sql

--changeset aomaoi:004-e2e-run-tracking dbms:postgresql splitStatements:false
--comment: Track shared-system test data independently of editable names.
CREATE TABLE e2e_runs (
    id VARCHAR(36) PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE users ADD COLUMN e2e_run_id VARCHAR(36) REFERENCES e2e_runs(id);
ALTER TABLE farm ADD COLUMN e2e_run_id VARCHAR(36) REFERENCES e2e_runs(id);
ALTER TABLE worker ADD COLUMN e2e_run_id VARCHAR(36) REFERENCES e2e_runs(id);
ALTER TABLE admin_profile ADD COLUMN e2e_run_id VARCHAR(36) REFERENCES e2e_runs(id);
ALTER TABLE work_logs ADD COLUMN e2e_run_id VARCHAR(36) REFERENCES e2e_runs(id);
ALTER TABLE audit_logs ADD COLUMN e2e_run_id VARCHAR(36) REFERENCES e2e_runs(id);

CREATE FUNCTION aomaoi_e2e_guard() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    run_id TEXT := NULLIF(current_setting('aomaoi.e2e_run', true), '');
    parent_run TEXT;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        IF OLD.e2e_run_id IS DISTINCT FROM run_id THEN
            RAISE EXCEPTION 'E2E ownership mismatch: cannot modify data outside this run';
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    NEW.e2e_run_id := run_id;
    IF TG_TABLE_NAME IN ('worker', 'admin_profile') THEN
        IF NEW.farm_id IS NOT NULL THEN
            SELECT e2e_run_id INTO parent_run FROM farm WHERE id = NEW.farm_id;
            IF parent_run IS DISTINCT FROM run_id THEN
                RAISE EXCEPTION 'E2E farm ownership mismatch';
            END IF;
        END IF;
        IF NEW.user_id IS NOT NULL THEN
            SELECT e2e_run_id INTO parent_run FROM users WHERE id = NEW.user_id;
            IF parent_run IS DISTINCT FROM run_id THEN
                RAISE EXCEPTION 'E2E user ownership mismatch';
            END IF;
        END IF;
    ELSIF TG_TABLE_NAME = 'work_logs' THEN
        SELECT e2e_run_id INTO parent_run FROM worker WHERE id = NEW.worker_id;
        IF parent_run IS DISTINCT FROM run_id THEN
            RAISE EXCEPTION 'E2E worker ownership mismatch';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER e2e_guard BEFORE INSERT OR UPDATE OR DELETE ON users FOR EACH ROW EXECUTE FUNCTION aomaoi_e2e_guard();
CREATE TRIGGER e2e_guard BEFORE INSERT OR UPDATE OR DELETE ON farm FOR EACH ROW EXECUTE FUNCTION aomaoi_e2e_guard();
CREATE TRIGGER e2e_guard BEFORE INSERT OR UPDATE OR DELETE ON worker FOR EACH ROW EXECUTE FUNCTION aomaoi_e2e_guard();
CREATE TRIGGER e2e_guard BEFORE INSERT OR UPDATE OR DELETE ON admin_profile FOR EACH ROW EXECUTE FUNCTION aomaoi_e2e_guard();
CREATE TRIGGER e2e_guard BEFORE INSERT OR UPDATE OR DELETE ON work_logs FOR EACH ROW EXECUTE FUNCTION aomaoi_e2e_guard();
CREATE TRIGGER e2e_guard BEFORE INSERT OR UPDATE OR DELETE ON audit_logs FOR EACH ROW EXECUTE FUNCTION aomaoi_e2e_guard();

--rollback DROP TRIGGER e2e_guard ON audit_logs;
--rollback DROP TRIGGER e2e_guard ON work_logs;
--rollback DROP TRIGGER e2e_guard ON admin_profile;
--rollback DROP TRIGGER e2e_guard ON worker;
--rollback DROP TRIGGER e2e_guard ON farm;
--rollback DROP TRIGGER e2e_guard ON users;
--rollback DROP FUNCTION aomaoi_e2e_guard();
--rollback ALTER TABLE audit_logs DROP COLUMN e2e_run_id;
--rollback ALTER TABLE work_logs DROP COLUMN e2e_run_id;
--rollback ALTER TABLE admin_profile DROP COLUMN e2e_run_id;
--rollback ALTER TABLE worker DROP COLUMN e2e_run_id;
--rollback ALTER TABLE farm DROP COLUMN e2e_run_id;
--rollback ALTER TABLE users DROP COLUMN e2e_run_id;
--rollback DROP TABLE e2e_runs;
