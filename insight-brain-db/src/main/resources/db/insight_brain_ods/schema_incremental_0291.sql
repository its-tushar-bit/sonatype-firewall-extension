-- Since 1.161
-- The new column is filled in by the java class (com.sonatype.insight.brain.migration.ProprietaryComponentNamePatternMigrator)
-- associated with this incremental script and it will be changed to NOT NULL by the next schema incremental script.
ALTER TABLE proprietary_component_name_pattern ADD COLUMN repository_id varchar(50) NULL;
