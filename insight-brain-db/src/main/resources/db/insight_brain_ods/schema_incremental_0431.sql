-- since 1.202
-- SaaS Compatible
-- CLM-37529: Add ACCESS_AUDIT_LOG permission to System Administrator built-in role

-- System Administrator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('3229ffe4e3a945c3bf11ab649ffa875e', '1b92fae3e55a411793a091fb821c422d', 'ACCESS_AUDIT_LOG');
