-- SaaS Compatible
-- CLM-39017: Add scan_health_config table for zero components failure configuration

CREATE TABLE IF NOT EXISTS scan_health_config (
  scan_health_config_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  owner_type varchar(20) NOT NULL,
  configuration_json TEXT NOT NULL,
  create_time timestamp NOT NULL,
  update_time timestamp NOT NULL,
  CONSTRAINT scan_health_config_pk PRIMARY KEY (scan_health_config_id),
  CONSTRAINT scan_health_config_owner_uk UNIQUE (owner_id, owner_type)
);

CREATE INDEX IF NOT EXISTS scan_health_config_owner_idx ON scan_health_config(owner_id);
