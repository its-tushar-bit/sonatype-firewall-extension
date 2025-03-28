-- since 1.190
-- SaaS Compatible

ALTER TABLE policy_waiver_request ALTER COLUMN waiver_reason_id DROP NOT NULL;
ALTER TABLE policy_waiver_request ADD COLUMN note_to_reviewer text NULL;
