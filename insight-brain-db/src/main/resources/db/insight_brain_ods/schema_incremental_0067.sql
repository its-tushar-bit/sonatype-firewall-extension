-- Since 1.14.0
SET SCHEMA insight_brain_ods;

INSERT INTO role (role_id, name, name_lowercase_no_whitespace, description, global) VALUES ('2cb71b3468d649789163ea2e212b541e', 'Application Evaluator', 'applicationevaluator', 'Allows to evaluate policies on applications.', FALSE);

INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('99fe1291b1274b169ce5c854dd856ad8', '1b92fae3e55a411793a091fb821c422d', 'EVALUATE_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('465f023ab44040deb2c5a3b7c3dd3c77', '1cddabf7fdaa47d6833454af10e0a3ef', 'EVALUATE_APPLICATION');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('ea7bb57e93e241acbf8da5ebcb5b0074', '2cb71b3468d649789163ea2e212b541e', 'EVALUATE_APPLICATION');

