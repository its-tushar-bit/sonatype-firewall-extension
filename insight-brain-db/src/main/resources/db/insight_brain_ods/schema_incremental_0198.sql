-- Since 1.101
ALTER TABLE policy_evaluation ALTER COLUMN initiator TYPE varchar(60);

ALTER TABLE source_control_event ALTER COLUMN initiator TYPE varchar(60);
