-- Since 1.65.0
SET SCHEMA insight_brain_ods;

CREATE TABLE schema_version (
  schema_version int NOT NULL
);
INSERT INTO schema_version (schema_version) VALUES (-1);
