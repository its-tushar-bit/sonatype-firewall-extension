-- since 1.203
-- SaaS Compatible
-- CLM-39100: GitHub App multi-installation support with is_active flag
ALTER TABLE github_app DROP CONSTRAINT IF EXISTS github_app_owner_id_uk;
ALTER TABLE github_app ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT false;

UPDATE github_app
SET is_active = true
WHERE owner_id IN (
    SELECT owner_id
    FROM source_control
    WHERE provider = 'GITHUB'
      AND authentication_type = 'GITHUB_APP'
)
  AND is_active = false;
