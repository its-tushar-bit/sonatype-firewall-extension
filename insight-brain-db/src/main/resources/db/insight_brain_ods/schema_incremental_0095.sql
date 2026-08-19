-- Since 1.20.0
INSERT INTO role_permission (role_permission_id, role_id, permission) SELECT REPLACE(RANDOM_UUID(),'-'), role_id, 'ADD_APPLICATION' FROM role_permission WHERE permission='WRITE';
