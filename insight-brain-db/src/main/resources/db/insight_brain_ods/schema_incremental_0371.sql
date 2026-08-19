-- Since 1.187
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS historical_telemetry_state (
  historical_telemetry_state_id VARCHAR(50) NOT NULL,

  batch_size INTEGER NOT NULL DEFAULT 1000,
  min_free_memory_mb INTEGER NOT NULL DEFAULT 10,
  cutoff_date DATE NOT NULL,

  created TIMESTAMP NOT NULL,
  start_time TIMESTAMP NULL,
  last_updated TIMESTAMP NULL,

  -- these 'last_record' fields are used to track the last record processed by the telemetry job
  last_record_time TIMESTAMP NULL,
  last_record_key VARCHAR(50) NULL,

  status VARCHAR(15) NOT NULL,

  CONSTRAINT historical_telemetry_state_pk PRIMARY KEY (historical_telemetry_state_id)
);
