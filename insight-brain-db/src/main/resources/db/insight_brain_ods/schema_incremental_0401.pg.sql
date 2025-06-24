-- SaaS Compatible

BEGIN;

LOCK TABLE inner_source_component IN EXCLUSIVE MODE;

-- Inserting data into inner_source_application from inner_source_component
INSERT INTO inner_source_application (inner_source_application_id, application_id, package_url)
SELECT
    c.inner_source_component_id,
    c.application_id,
    c.package_url
FROM inner_source_component c
ON CONFLICT (inner_source_application_id) DO NOTHING;

-- Inserting data into inner_source_version from inner_source_component if latest_version is not null
INSERT INTO inner_source_version (inner_source_version_id, inner_source_application_id, latest_version, stage_type_id)
SELECT
    c.inner_source_component_id,
    c.inner_source_component_id,
    c.latest_version,
    NULL
FROM inner_source_component c
WHERE c.latest_version IS NOT NULL
ON CONFLICT (inner_source_version_id) DO NOTHING;

COMMIT;
