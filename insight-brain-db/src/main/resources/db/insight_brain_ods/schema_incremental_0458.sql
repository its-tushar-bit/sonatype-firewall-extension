-- SaaS Compatible
-- CLM-39685: Add webhook_secret column to github_app for relay auto-registration during the
-- manifest wizard. GitHub returns this secret in the manifest-conversion response when the
-- manifest carries hook_attributes; IQ stores it encrypted (PasswordHandler) and forwards
-- the plaintext to the relay at installation-setup time so the relay can verify webhook
-- signatures. Nullable because Apps registered before this column existed (or via flows
-- that did not set hook_attributes) have no secret to store.

ALTER TABLE github_app ADD COLUMN IF NOT EXISTS webhook_secret VARCHAR(2000);
