-- Since 1.18.0
DROP INDEX repository_policy_violation_hash_idx;
CREATE INDEX repository_policy_violation_pathname_idx ON repository_policy_violation(pathname);
