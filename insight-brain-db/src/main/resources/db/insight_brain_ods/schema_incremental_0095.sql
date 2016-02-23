-- Since 1.20.0
SET SCHEMA insight_brain_ods;

INSERT INTO role_permission (role_permission_id, role_id, permission) SELECT REPLACE(RANDOM_UUID(),'-'), role_id, 'ADD_APPLICATION' FROM role_permission WHERE permission='WRITE';
