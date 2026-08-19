-- since 1.203
-- SaaS Compatible
-- EI-440: Add retry tracking fields to historical_telemetry_state table

ALTER TABLE historical_telemetry_state ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE historical_telemetry_state ADD COLUMN IF NOT EXISTS last_retry_time TIMESTAMP NULL;
