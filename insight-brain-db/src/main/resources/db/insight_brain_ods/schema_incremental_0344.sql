-- Since 1.180
-- SaaS Compatible
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('e5626c1fe4304296b11d209f6cd2f158', 'SBOM Exporter', 'sbomexporter', 700, 'Exports SBOMs from applications.', FALSE, TRUE);
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in) VALUES ('602c74a3286942b5a7d7e978e5e2bea8', 'SBOM Importer', 'sbomimporter', 800, 'Imports SBOMs into applications.', FALSE, TRUE);
-- Policy Administrator role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('8e7efbdba11944a381a4da4025690bee', 'b9646757e98e486da7d730025f5245f8', 'EXPORT_SBOM');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('a312b9e5255f43a985525a2afdc2e450', 'b9646757e98e486da7d730025f5245f8', 'IMPORT_SBOM');
-- Owner role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('34333499b52546cd94b24a0cae57d331', '1cddabf7fdaa47d6833454af10e0a3ef', 'EXPORT_SBOM');
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('bfa1ffc66c8c4070ad1e765334e30bfa', '1cddabf7fdaa47d6833454af10e0a3ef', 'IMPORT_SBOM');
-- SBOM Exporter role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('4f72fbabc5524ed8be85ef91c44a64b1', 'e5626c1fe4304296b11d209f6cd2f158', 'EXPORT_SBOM');
-- SBOM Importer role
INSERT INTO role_permission (role_permission_id, role_id, permission) VALUES ('6db921e4c8dc4215845493b5d57b6a01', '602c74a3286942b5a7d7e978e5e2bea8', 'IMPORT_SBOM');
