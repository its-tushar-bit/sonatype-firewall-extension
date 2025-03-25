-- since 1.189
-- SaaS Compatible

CREATE TABLE policy_waiver_request (
  policy_waiver_request_id varchar(50) NOT NULL,
  hash varchar(20) NULL,  -- null if waiver request applies to all components of app/org
  policy_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  -- record of the policy constraints/conditions that were violated
  constraint_facts_json text NOT NULL,
  associated_package_url varchar(1000) NULL,
  component_match_strategy varchar(30) NOT NULL,
  comment varchar(1000) NULL,
  request_time timestamp NOT NULL,
  expiry_time timestamp default NULL,
  component_upgrade_available boolean,
  waiver_reason_id varchar(50) NOT NULL,
  expire_when_remediation_available boolean DEFAULT false,
  status varchar(20) NOT NULL,
  -- ID of the policy waiver created on policy waiver request approval
  policy_waiver_id varchar(50) NULL,
  requester_id varchar(60) NOT NULL,
  requester_name varchar(210) NOT NULL,
  reviewer_id varchar(60) NULL,
  reviewer_name varchar(210) NULL,
  review_time timestamp NULL,
  request_reason text NULL,
  rejection_reason text NULL,
  CONSTRAINT policy_waiver_request_pk PRIMARY KEY (policy_waiver_request_id),
  CONSTRAINT policy_waiver_request_policy_fk FOREIGN KEY (policy_id) REFERENCES policy(policy_id)
     ON DELETE CASCADE,
  CONSTRAINT policy_waiver_request_policy_waiver_fk FOREIGN KEY (policy_waiver_id) REFERENCES policy_waiver(policy_waiver_id)
     ON DELETE CASCADE
);
CREATE INDEX policy_waiver_request_owner_id_idx ON policy_waiver_request(owner_id);
