-- Since 1.63
CREATE TABLE data_retention_policy (
  data_retention_policy_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  context_id varchar(30) NOT NULL,
  purging_enabled boolean NOT NULL,
  max_count smallint,
  max_age_in_days smallint,
  CONSTRAINT data_retention_policy_pk PRIMARY KEY (data_retention_policy_id),
  CONSTRAINT data_retention_policy_uk UNIQUE KEY (owner_id, context_id)
);
-- Add  default retention policies for root organization
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a0', 'ROOT_ORGANIZATION_ID', 'develop', false, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a1', 'ROOT_ORGANIZATION_ID', 'build', false, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a2', 'ROOT_ORGANIZATION_ID', 'stage-release', false, 90);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a3', 'ROOT_ORGANIZATION_ID', 'release', false, 3650);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a4', 'ROOT_ORGANIZATION_ID', 'operate', false, 3650);
INSERT INTO data_retention_policy (data_retention_policy_id, owner_id, context_id, purging_enabled, max_age_in_days) VALUES('5575c590071c438c95ff3980ee9c71a5', 'ROOT_ORGANIZATION_ID', 'continuous-monitoring', false, 90);
