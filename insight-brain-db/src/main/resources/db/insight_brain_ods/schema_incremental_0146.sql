-- Since 1.76.0
-- add columns for policy waiver details
ALTER TABLE repository_policy_violation ADD COLUMN policy_waiver_id varchar(50) NULL;
ALTER TABLE repository_policy_violation ADD COLUMN policy_waiver_comment varchar(1000) NULL;
ALTER TABLE repository_policy_violation ADD COLUMN waive_time timestamp NULL;
