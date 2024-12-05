-- Since 1.186
-- SaaS Compatible
DELETE FROM system_configuration_property WHERE name = 'sbomPolicies';
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('64716cfdb4b741e793bdd7fbc44bc93p', 'sbomPolicies', 'true');
