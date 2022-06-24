-- Since 1.140
ALTER TABLE policy_waiver ADD COLUMN associated_package_url varchar(1000) NULL;
ALTER TABLE policy_waiver ADD COLUMN component_match_strategy varchar(30) NULL;

UPDATE policy_waiver
SET component_match_strategy='ALL_COMPONENTS'
WHERE component_match_strategy IS NULL AND hash IS NULL;

UPDATE policy_waiver
SET component_match_strategy='EXACT_COMPONENT'
WHERE component_match_strategy IS NULL AND hash IS NOT NULL;
