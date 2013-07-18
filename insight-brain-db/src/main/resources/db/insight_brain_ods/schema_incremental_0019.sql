SET SCHEMA insight_brain_ods;

ALTER TABLE label
  DROP CONSTRAINT label_application_fk;
ALTER TABLE label
  ALTER COLUMN application_id RENAME TO owner_id;

ALTER TABLE component_label
  DROP CONSTRAINT component_label_application_fk;
ALTER TABLE component_label
  ALTER COLUMN application_id RENAME TO owner_id;