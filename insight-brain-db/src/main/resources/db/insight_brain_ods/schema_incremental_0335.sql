-- since 1.174
-- SaaS Compatible

CREATE INDEX IF NOT EXISTS policy_violation_open_time_idx
    ON policy_violation (open_time);
