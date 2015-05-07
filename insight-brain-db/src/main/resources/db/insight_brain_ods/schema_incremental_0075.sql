-- Since 1.15.0
SET SCHEMA insight_brain_ods;

ALTER TABLE role
  ADD COLUMN built_in boolean NULL;

UPDATE role SET built_in=TRUE;

ALTER TABLE role
  ALTER COLUMN built_in boolean DEFAULT false NOT NULL;
