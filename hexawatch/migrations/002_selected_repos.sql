-- Store the full selected-repo objects (including remoteUrl) so the extension
-- can register content scripts dynamically without re-fetching from IQ on
-- every service-worker startup. selectedRepoIds stays for back-compat but is
-- derived from this on write.
ALTER TABLE extension_config
  ADD COLUMN IF NOT EXISTS selected_repos jsonb NOT NULL DEFAULT '[]'::jsonb;
