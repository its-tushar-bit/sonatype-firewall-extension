-- Add GPG signing key and passphrase to source_control_configuration
-- SaaS Compatible

ALTER TABLE source_control_configuration ADD COLUMN IF NOT EXISTS gpg_signing_key VARCHAR(1024) NULL;
ALTER TABLE source_control_configuration ADD COLUMN IF NOT EXISTS gpg_passphrase VARCHAR(1024) NULL;
