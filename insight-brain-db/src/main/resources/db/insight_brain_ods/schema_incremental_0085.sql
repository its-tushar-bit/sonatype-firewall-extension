-- Since 1.16.0
SET SCHEMA insight_brain_ods;

CREATE TABLE schema_info (
  schema_info_id varchar(50) NOT NULL,
  drools_code_version int NOT NULL,
  CONSTRAINT schema_info_pk PRIMARY KEY (schema_info_id)
);
-- NOTE: drools_code_version will be set by PolicyDroolsCodeGenerator after migration
INSERT INTO schema_info (schema_info_id, drools_code_version) VALUES ('1', 0);
