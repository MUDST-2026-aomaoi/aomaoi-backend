--liquibase formatted sql

--changeset aomaoi:006-lock-worker-farm-after-financial-activity dbms:postgresql splitStatements:false
--comment: Keep historical wages and advances assigned to the farm where they were recorded.
CREATE FUNCTION aomaoi_guard_worker_farm_change() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.farm_id IS DISTINCT FROM OLD.farm_id AND
       (EXISTS (SELECT 1 FROM work_logs WHERE worker_id = OLD.id) OR
        EXISTS (SELECT 1 FROM salary_advances WHERE worker_id = OLD.id)) THEN
        RAISE EXCEPTION 'Cannot change farm after work logs or salary advances exist for worker %', OLD.id;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER guard_worker_farm_change BEFORE UPDATE OF farm_id ON worker
    FOR EACH ROW EXECUTE FUNCTION aomaoi_guard_worker_farm_change();

--rollback DROP TRIGGER guard_worker_farm_change ON worker;
--rollback DROP FUNCTION aomaoi_guard_worker_farm_change();
