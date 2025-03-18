-- since 1.189
-- SaaS Compatible

ALTER TABLE policy_waiver ADD COLUMN status varchar(20) NULL;
ALTER TABLE policy_waiver ADD COLUMN approver_id varchar(60) NULL;
ALTER TABLE policy_waiver ADD COLUMN approver_name varchar(210) NULL;
ALTER TABLE policy_waiver ADD COLUMN requester_id varchar(60) NULL;
ALTER TABLE policy_waiver ADD COLUMN requester_name varchar(210) NULL;
ALTER TABLE policy_waiver ADD COLUMN approval_time timestamp NULL;
ALTER TABLE policy_waiver ADD COLUMN request_reason text NULL;
ALTER TABLE policy_waiver ADD COLUMN rejection_reason text NULL;
