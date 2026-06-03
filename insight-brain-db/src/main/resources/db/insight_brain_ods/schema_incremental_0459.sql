-- SaaS Compatible
-- Track relay-link health for each GitHub App so transient registration failures during
-- installation-setup auto-registration can be retried by the polling-cycle pre-flight and
-- by the hourly slow-sweep job (see RelayPollingService and RelayLinkRetrySweepTask).
ALTER TABLE github_app ADD COLUMN IF NOT EXISTS relay_link_state varchar(16) NOT NULL DEFAULT 'UNREGISTERED';
ALTER TABLE github_app ADD COLUMN IF NOT EXISTS relay_link_attempts int NOT NULL DEFAULT 0;
