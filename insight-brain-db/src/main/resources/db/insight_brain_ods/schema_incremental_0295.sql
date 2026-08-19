-- Since 1.162
DELETE FROM system_configuration_property
WHERE system_configuration_property_id = 'bc973eece2b811edb5ea0242ac120002';

INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('531170d2fc694631a05f4d8ab61e53b9', 'alpObservedLicenseDetectionEnabled', 'false');
