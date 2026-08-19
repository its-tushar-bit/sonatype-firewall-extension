-- Since 1.109
-- Add more default retention policies for root organization
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days)
VALUES('5575c590071c438c95ff3980ee9c71a7', 'ROOT_ORGANIZATION_ID', 'source', true, 90);
