-- Since 1.32.0
SET SCHEMA insight_brain_ods;

ALTER TABLE repository_component
  DROP COLUMN can_be_quarantined;
