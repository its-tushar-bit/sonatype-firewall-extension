SET SCHEMA insight_brain_ods;

ALTER TABLE hash_gav
  ADD COLUMN comment varchar(1000) NULL;
