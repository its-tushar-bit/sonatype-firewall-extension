-- Since 1.11
SET SCHEMA insight_brain_ods;

CREATE INDEX policy_violation_policy_id_idx ON policy_violation(policy_id);
CREATE INDEX policy_violation_hash_idx ON policy_violation(hash);
