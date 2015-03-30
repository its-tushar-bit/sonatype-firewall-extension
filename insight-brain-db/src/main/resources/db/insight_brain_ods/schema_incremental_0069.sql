-- Since 1.14.0
SET SCHEMA insight_brain_ods;

INSERT INTO role (role_id, name, name_lowercase_no_whitespace, description, global) VALUES ('90c7c98683b4471cb77a916744540bcc', 'Component Evaluator', 'componentevaluator', 'Allows to evaluate policies on components.', FALSE);

INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('d9373880213342f2b4e56e3dea94f50c', '1b92fae3e55a411793a091fb821c422d', 'EVALUATE_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('33fbb19ce93e4420a0ebcd846b0705d5', '1cddabf7fdaa47d6833454af10e0a3ef', 'EVALUATE_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('61159b674eb94cdcb00ebdec77a47373', '1da70fae1fd54d6cb7999871ebdb9a36', 'EVALUATE_COMPONENT');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('f198535bdf2549d38417534e38ae3cda', '90c7c98683b4471cb77a916744540bcc', 'EVALUATE_COMPONENT');
