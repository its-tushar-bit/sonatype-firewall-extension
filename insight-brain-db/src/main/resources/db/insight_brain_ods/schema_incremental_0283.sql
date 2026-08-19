-- Since 1.159
ALTER TABLE organization ADD COLUMN waived_component_upgrade_stage_type_id varchar(30) NULL;
ALTER TABLE policy_waiver ADD COLUMN component_upgrade_available boolean;