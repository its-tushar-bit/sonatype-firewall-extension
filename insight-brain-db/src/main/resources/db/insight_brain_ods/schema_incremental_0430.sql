-- Since 1.202
-- SaaS Compatible

-- EI-1097: Add is_remediated_by_version_change column to policy_violation table
-- This column tracks whether a policy violation was remediated by version change
-- (upgrade/downgrade) vs. component removal

ALTER TABLE policy_violation
ADD COLUMN is_remediated_by_version_change BOOLEAN DEFAULT NULL;
