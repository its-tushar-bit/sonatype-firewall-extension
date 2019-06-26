-- Since 1.67.0
INSERT INTO migration_tracker (migration_tracker_id, version) SELECT 'policy-drools-code', drools_code_version FROM schema_info;
INSERT INTO migration_tracker (migration_tracker_id, version) SELECT 'policy-json', policy_json_version FROM schema_info;

DROP TABLE schema_info;
