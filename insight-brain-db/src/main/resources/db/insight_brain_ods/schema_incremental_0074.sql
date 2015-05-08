-- Since 1.15.0
SET SCHEMA insight_brain_ods;

-- Administrator role
DELETE FROM role_permission WHERE role_permission_id = '1c5c83c335e74a138ee1ae8fa7869da7';
DELETE FROM role_permission WHERE role_permission_id = '1cd867a40a574ce0b46dd22c9d650d1d';
DELETE FROM role_permission WHERE role_permission_id = '99fe1291b1274b169ce5c854dd856ad8';
DELETE FROM role_permission WHERE role_permission_id = 'd9373880213342f2b4e56e3dea94f50c';

-- CLM Administrator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('1539fa2c5afd4cd4b7102ef6c8d0bf6b', 'b9646757e98e486da7d730025f5245f8', 'WRITE');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('c49843ffa4ae4bb68c3e35b25244486e', 'b9646757e98e486da7d730025f5245f8', 'READ');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('6da52edbbc554b3ab4dd502e30a80acd', 'b9646757e98e486da7d730025f5245f8', 'EVALUATE_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('e07e0e487c074a4fa95a1abde2f68aed', 'b9646757e98e486da7d730025f5245f8', 'EVALUATE_COMPONENT');

INSERT INTO membership_mapping (membership_mapping_id, context_id, role_id, member_name, member_type) VALUES ('c20a1df68fa948b787f3d1962411fb50', 'global', 'b9646757e98e486da7d730025f5245f8', 'admin', 'USER');
