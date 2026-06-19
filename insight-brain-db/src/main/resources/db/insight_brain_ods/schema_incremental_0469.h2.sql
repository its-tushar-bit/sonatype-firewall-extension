-- SaaS Compatible
-- CLM-40771 (H2-only, migration 0469): Add idempotency_key column to consumption_events.
-- H2 does not support partial indexes, so a standard UNIQUE INDEX is used. H2 allows
-- multiple NULLs in a UNIQUE index by default, which correctly mirrors the
-- PostgreSQL partial-index semantics (WHERE idempotency_key IS NOT NULL).

ALTER TABLE consumption_events ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_consumption_events_idempotency_key
  ON consumption_events (idempotency_key);
