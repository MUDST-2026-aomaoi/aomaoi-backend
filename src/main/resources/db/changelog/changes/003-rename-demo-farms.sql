--liquibase formatted sql

--changeset aomaoi:003-rename-demo-farms dbms:postgresql context:demo
--comment: Remove the sample label from seeded farm names without changing farm IDs.

UPDATE farm AS f
SET name = seed.new_name
FROM (VALUES
    ('demo_admin01', 'ฟาร์มเพื่อพ่อ (ตัวอย่าง)', 'ฟาร์มเพื่อพ่อ'),
    ('demo_admin02', 'ไร่สุขใจ (ตัวอย่าง)', 'ไร่สุขใจ'),
    ('demo_admin03', 'ไร่แสงทอง (ตัวอย่าง)', 'ไร่แสงทอง'),
    ('demo_admin04', 'ฟาร์มอ้อยบ้านสวน (ตัวอย่าง)', 'ฟาร์มอ้อยบ้านสวน'),
    ('demo_admin05', 'ไร่พอเพียง (ตัวอย่าง)', 'ไร่พอเพียง')
) AS seed(username, old_name, new_name)
JOIN users AS u ON u.username = seed.username AND u.role = 'admin'
JOIN admin_profile AS ap ON ap.user_id = u.id
WHERE f.id = ap.farm_id AND f.name = seed.old_name;

--rollback UPDATE farm AS f SET name = seed.old_name FROM (VALUES ('demo_admin01', 'ฟาร์มเพื่อพ่อ (ตัวอย่าง)', 'ฟาร์มเพื่อพ่อ'), ('demo_admin02', 'ไร่สุขใจ (ตัวอย่าง)', 'ไร่สุขใจ'), ('demo_admin03', 'ไร่แสงทอง (ตัวอย่าง)', 'ไร่แสงทอง'), ('demo_admin04', 'ฟาร์มอ้อยบ้านสวน (ตัวอย่าง)', 'ฟาร์มอ้อยบ้านสวน'), ('demo_admin05', 'ไร่พอเพียง (ตัวอย่าง)', 'ไร่พอเพียง')) AS seed(username, old_name, new_name) JOIN users AS u ON u.username = seed.username AND u.role = 'admin' JOIN admin_profile AS ap ON ap.user_id = u.id WHERE f.id = ap.farm_id AND f.name = seed.new_name;
