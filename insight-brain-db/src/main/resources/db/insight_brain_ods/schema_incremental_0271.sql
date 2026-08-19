ALTER TABLE schema_version ADD COLUMN IF NOT EXISTS data_store_id varchar(32);
UPDATE schema_version SET data_store_id = 'insight_brain_ods';
ALTER TABLE schema_version ALTER COLUMN data_store_id SET NOT NULL;
