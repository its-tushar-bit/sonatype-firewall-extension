-- since 1.164
-- This script should have been since https://github.com/sonatype/insight-brain/pull/9561/files#diff-30b10f544a86e121d69ca488daa6467942910f32111c25193563556590cdc272
-- and released in 1.162, but it was lost in the process of syncing the feature branch with main

-- SaaS Compatible
-- Insert the old column with no values if it does not exists, in order for the following insert check to work
ALTER TABLE organization ADD COLUMN IF NOT EXISTS waived_component_upgrade_stage_type_id varchar(30) NULL;

-- Only insert the new property enabled if it does not exists and if it was enabled through the previously used column
INSERT INTO "system_configuration_property" ("system_configuration_property_id", "name", "value")
SELECT 'c9858f78e1804212845dee3e7d1b6fee', 'waivedComponentUpgradeMonitoringEnabled', 'true'
WHERE NOT EXISTS (SELECT 1 FROM "system_configuration_property" WHERE "name" = 'waivedComponentUpgradeMonitoringEnabled')
  AND EXISTS (SELECT 1 FROM "organization" WHERE "waived_component_upgrade_stage_type_id" IS NOT NULL);

-- This column is not used anymore in the code, is replaced by the above system configuration property.
-- The entity at this point (since 1.162) does not have this field anymore either so it should be safe to drop.
-- It could have also been just added for the script to run cleanly, in which case is also safe to drop.
ALTER TABLE organization DROP COLUMN IF EXISTS waived_component_upgrade_stage_type_id;
