-- Since 1.21.0
ALTER TABLE schema_info
    ADD COLUMN policy_json_version int NOT NULL DEFAULT 0;
ALTER TABLE schema_info
    ALTER COLUMN policy_json_version int NOT NULL;
