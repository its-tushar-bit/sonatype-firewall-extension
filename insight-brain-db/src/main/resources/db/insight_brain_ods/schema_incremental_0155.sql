-- Since 1.83.0
INSERT INTO role_permission (role_permission_id, role_id, permission)
  SELECT CONCAT(SUBSTRING(role_permission_id, 0, 27), 'bbbbb'), role_id, 'WAIVE_POLICY_VIOLATIONS' FROM role_permission
    WHERE permission='WRITE';
