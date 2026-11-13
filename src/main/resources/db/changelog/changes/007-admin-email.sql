--liquibase formatted sql

--changeset aomaoi:007-admin-email dbms:postgresql
--comment: Admin e-mail address used for the end-of-day summary. Nullable so existing admins keep working.
ALTER TABLE admin_profile ADD COLUMN email VARCHAR(255);

--rollback ALTER TABLE admin_profile DROP COLUMN email;
