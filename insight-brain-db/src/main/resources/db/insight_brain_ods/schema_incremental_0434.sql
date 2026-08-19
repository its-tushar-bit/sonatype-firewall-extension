-- since 1.202
-- SaaS Compatible
-- CLM-34850: Create default source control configuration singleton for upgrade path
-- This ensures customers upgrading with existing source control records get the configuration singleton

-- Only create singleton if:
-- 1. At least one source control record exists (customer has configured source control)
-- 2. The singleton doesn't already exist (idempotent, safe to run multiple times)
INSERT INTO source_control_configuration (
    source_control_configuration_id,
    clone_directory,
    git_timeout_seconds,
    use_username_in_repository_clone_url,
    default_branch_monitoring_interval_hours,
    pull_request_monitoring_interval_seconds
)
SELECT
    'source-control-configuration',
    'source-control',
    0,
    false,
    24,
    60
WHERE EXISTS (
    SELECT 1 FROM source_control LIMIT 1
)
AND NOT EXISTS (
    SELECT 1 FROM source_control_configuration
    WHERE source_control_configuration_id = 'source-control-configuration'
);
