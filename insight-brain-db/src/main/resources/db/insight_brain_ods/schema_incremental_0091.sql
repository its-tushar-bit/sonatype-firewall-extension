-- Since 1.17.0
SET SCHEMA insight_brain_ods;

ALTER TABLE repository ADD COLUMN quarantine_enabled bool DEFAULT false NOT NULL;
