-- Since 1.82.0
INSERT INTO role_permission (role_permission_id, role_id, permission)
  SELECT CONCAT(SUBSTRING(role_permission_id, 0, 27), 'aaaaa'), role_id, 'EDIT_ACCESS_CONTROL' FROM role_permission
    WHERE permission='WRITE';
