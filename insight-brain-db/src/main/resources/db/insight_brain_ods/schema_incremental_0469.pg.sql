-- SaaS Compatible
-- CLM-40771 (Postgres-only, migration 0469): Add idempotency_key column and a partial unique index.
-- NULL values are excluded so that events without a key (legacy paths, sessionless
-- background events without sufficient ctx) can co-exist without uniqueness conflicts.
--
-- The DAO emits ON CONFLICT (idempotency_key) WHERE (idempotency_key IS NOT NULL)
-- DO NOTHING — the predicate must match this index for Postgres to use it as the
-- conflict arbiter (see ConsumptionEventDAO#recordEvent).

ALTER TABLE consumption_events ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_consumption_events_idempotency_key
  ON consumption_events (idempotency_key)
  WHERE idempotency_key IS NOT NULL;
