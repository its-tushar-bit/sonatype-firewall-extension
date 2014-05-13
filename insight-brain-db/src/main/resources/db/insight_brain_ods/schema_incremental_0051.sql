-- Since 1.11
SET SCHEMA insight_brain_ods;

ALTER TABLE application_component
  ADD COLUMN time datetime NULL;
UPDATE application_component ac
  SET ac.time=(SELECT pe.time FROM policy_evaluation pe WHERE pe.application_id=ac.application_id AND pe.stage_type_id=ac.stage_type_id ORDER BY pe.time DESC LIMIT 1);
ALTER TABLE application_component
  ALTER COLUMN time datetime NOT NULL;
  
CREATE INDEX application_component_time_idx ON application_component(time);
