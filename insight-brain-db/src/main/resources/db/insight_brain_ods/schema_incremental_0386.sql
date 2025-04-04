-- since 1.190
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS cluster_identification (
  cluster_identification_id VARCHAR(50) NOT NULL,

  assigned_cluster_id VARCHAR(128),
  assigned_telemetry_id VARCHAR(15) NOT NULL,
  tamper_code VARCHAR(50) NOT NULL,

  base_url_hash VARCHAR(50) NOT NULL,
  last_calculated_cluster_id VARCHAR(128),

  created TIMESTAMP NOT NULL,
  last_updated TIMESTAMP,

  CONSTRAINT cluster_identification_pk PRIMARY KEY (cluster_identification_id)
  );
