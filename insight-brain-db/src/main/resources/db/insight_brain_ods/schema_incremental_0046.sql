-- Since 1.11
SET SCHEMA insight_brain_ods;

UPDATE label SET color='white' WHERE color IS NULL;
UPDATE tag SET color='white' WHERE color IS NULL;

ALTER TABLE label
   ALTER COLUMN color varchar(20) NOT NULL;
ALTER TABLE tag
   ALTER COLUMN color varchar(20) NOT NULL;
