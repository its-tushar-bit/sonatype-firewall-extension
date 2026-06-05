-- Since 1.205
-- SaaS Compatible
-- EI-1272: Add last_telemetry_emitted_date to gate CONDITION_TYPE_VIOLATION_AUDIT and TIME_TO_LEGACY_VIOLATION_AUDIT telemetry to once per calendar day per violation
ALTER TABLE policy_violation ADD COLUMN IF NOT EXISTS last_telemetry_emitted_date date;
