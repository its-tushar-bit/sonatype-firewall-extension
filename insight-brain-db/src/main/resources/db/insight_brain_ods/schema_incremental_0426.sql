-- Since 1.202
-- SaaS Compatible
-- Note: github_organization_name and last_updated_at are created in schema_incremental_0424 for new installations
-- For existing installations from 0424 without these columns, add them here
ALTER TABLE source_control DROP CONSTRAINT IF EXISTS source_control_github_app_fk;
ALTER TABLE source_control DROP COLUMN IF EXISTS github_app_id;
ALTER TABLE github_app ADD COLUMN IF NOT EXISTS github_organization_name VARCHAR(255);
ALTER TABLE github_app ADD COLUMN IF NOT EXISTS last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update any NULL values (for existing rows from before this migration)
UPDATE github_app SET github_organization_name = '' WHERE github_organization_name IS NULL;
UPDATE github_app SET last_updated_at = CURRENT_TIMESTAMP WHERE last_updated_at IS NULL;

-- Ensure columns are NOT NULL
ALTER TABLE github_app ALTER COLUMN github_organization_name SET NOT NULL;
ALTER TABLE github_app ALTER COLUMN last_updated_at SET NOT NULL;

-- Remove DEFAULT for H2 compatibility (H2 to PostgreSQL export compatibility)
ALTER TABLE github_app ALTER COLUMN last_updated_at DROP DEFAULT;

-- Make installation_id nullable to support pending installations
ALTER TABLE github_app ALTER COLUMN installation_id DROP NOT NULL;

-- Since 1.202
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS github_app_installation_state
(
    github_app_installation_state_id VARCHAR(50) NOT NULL,
    state_token                      VARCHAR(255) NOT NULL,
    github_app_id                    VARCHAR(50) NOT NULL,
    expires_at                       TIMESTAMP NOT NULL,
    created_at                       TIMESTAMP NOT NULL,
    CONSTRAINT github_app_installation_state_pk PRIMARY KEY (github_app_installation_state_id),
    CONSTRAINT github_app_installation_state_state_token_uk UNIQUE (state_token),
    CONSTRAINT fk_installation_state_github_app FOREIGN KEY (github_app_id) REFERENCES github_app(github_app_id)
);

-- Since 1.202
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS github_app_registration_state
(
    github_app_registration_state_id VARCHAR(50) NOT NULL,
    state_token                      VARCHAR(255) NOT NULL,
    owner_id                         VARCHAR(50) NOT NULL,
    github_organization_name         VARCHAR(255) NULL,
    expires_at                       TIMESTAMP NOT NULL,
    created_at                       TIMESTAMP NOT NULL,
    CONSTRAINT github_app_registration_state_pk PRIMARY KEY (github_app_registration_state_id),
    CONSTRAINT github_app_registration_state_state_token_uk UNIQUE (state_token)
);

-- EI-1097: Add is_remediated_by_version_change column to policy_violation table
-- This column tracks whether a policy violation was remediated by version change
-- (upgrade/downgrade) vs. component removal

-- SaaS Compatible

-- Add the column
ALTER TABLE policy_violation
ADD COLUMN is_remediated_by_version_change BOOLEAN DEFAULT NULL;

-- Add descriptive comment
COMMENT ON COLUMN policy_violation.is_remediated_by_version_change
IS 'Indicates if remediation was due to version change (upgrade/downgrade) vs. component removal. NULL means unknown/not tracked.';
