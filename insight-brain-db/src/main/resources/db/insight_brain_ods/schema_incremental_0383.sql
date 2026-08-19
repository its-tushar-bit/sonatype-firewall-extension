-- since 1.189
-- SaaS Compatible

ALTER TABLE policy_waiver DROP COLUMN status;
ALTER TABLE policy_waiver DROP COLUMN approver_id;
ALTER TABLE policy_waiver DROP COLUMN approver_name;
ALTER TABLE policy_waiver DROP COLUMN requester_id;
ALTER TABLE policy_waiver DROP COLUMN requester_name;
ALTER TABLE policy_waiver DROP COLUMN approval_time;
ALTER TABLE policy_waiver DROP COLUMN request_reason;
ALTER TABLE policy_waiver DROP COLUMN rejection_reason;

