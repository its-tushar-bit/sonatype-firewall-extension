SET SCHEMA insight_brain_ods;

ALTER TABLE hash_gav
  ADD COLUMN create_time datetime NULL;
