-- Since 1.183
-- SaaS Compatible
DELETE FROM membership_mapping WHERE role_id = 'e5626c1fe4304296b11d209f6cd2f158' OR role_id = '602c74a3286942b5a7d7e978e5e2bea8';
DELETE FROM role_permission WHERE permission = 'EXPORT_SBOM' OR permission = 'IMPORT_SBOM';
DELETE FROM role WHERE name = 'SBOM Exporter' OR name = 'SBOM Importer';
DELETE FROM system_configuration_property WHERE name = 'secureSharing';
