SET SCHEMA insight_brain_ods;

ALTER TABLE tag
  ADD COLUMN color varchar(20) NULL;