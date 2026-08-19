-- Since 1.104
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('0df46317c031440795007f4ce9c7f002', 'Legal Reviewer', 'legalreviewer', 600, 'Reviews legal obligations for components licenses.', FALSE, TRUE);
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('7b87527087c24a3c9ba81a55d7da7c0c', 'b9646757e98e486da7d730025f5245f8', 'LEGAL_REVIEWER');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('629fe173516645fda0121b2a6602ed0f', '1cddabf7fdaa47d6833454af10e0a3ef', 'LEGAL_REVIEWER');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('6c521879906a4fdf8d27c652787243b4', '0df46317c031440795007f4ce9c7f002', 'LEGAL_REVIEWER');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('9c3d3a466bed410fa8d8c8801f3a0c13', '0df46317c031440795007f4ce9c7f002', 'WRITE');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('4209ad3cdcfd474b865c51c0d664ea2a', '0df46317c031440795007f4ce9c7f002', 'READ');
