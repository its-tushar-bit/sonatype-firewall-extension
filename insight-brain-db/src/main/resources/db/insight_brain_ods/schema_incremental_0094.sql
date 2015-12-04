-- Since 1.18.0
SET SCHEMA insight_brain_ods;

ALTER TABLE repository
    ADD COLUMN format varchar(50);
