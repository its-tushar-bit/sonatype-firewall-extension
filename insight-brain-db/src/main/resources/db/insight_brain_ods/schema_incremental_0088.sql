-- Since 1.17.0
CREATE TABLE repository_manager (
  repository_manager_id varchar(50) NOT NULL,
  instance_id varchar(50) NOT NULL,
  CONSTRAINT repository_manager_pk PRIMARY KEY (repository_manager_id),
  CONSTRAINT repository_manager_uk UNIQUE KEY (instance_id)
);

CREATE TABLE repository (
  repository_id varchar(50) NOT NULL,
  repository_manager_id varchar(50) NOT NULL,
  public_id varchar(500) NOT NULL,
  enabled bool DEFAULT true NOT NULL,
  CONSTRAINT repository_pk PRIMARY KEY (repository_id),
  CONSTRAINT repository_uk UNIQUE KEY (repository_manager_id, public_id),
  CONSTRAINT repository_repository_manager_fk FOREIGN KEY (repository_manager_id) REFERENCES repository_manager(repository_manager_id)
);

CREATE TABLE repository_component (
  repository_component_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  time datetime NOT NULL,
  hash varchar(20) NOT NULL,
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000), -- the component identifier coordinates stored in json format
  match_state_id varchar(20) NOT NULL,
  identification_source_id varchar(20) NOT NULL,
  last_evaluation_time datetime NOT NULL,
  can_be_quarantined bool DEFAULT false NOT NULL,
  quarantine_time datetime,
  unquarantine_time datetime,
  CONSTRAINT repository_component_pk PRIMARY KEY (repository_component_id),
  CONSTRAINT repository_component_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id),
  CONSTRAINT repository_component_uk UNIQUE KEY (repository_id, hash, component_id_coordinates_json)
);
CREATE INDEX repository_component_hash_idx ON repository_component(hash);

CREATE TABLE repository_policy_violation (
  repository_policy_violation_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  time datetime NOT NULL,
  policy_id varchar(50) NOT NULL,
  policy_name varchar(60) NOT NULL, -- the policy name as it was when the policy violation was generated
  threat_level smallint(2) NOT NULL,
  threat_category varchar(20) NOT NULL,
  hash varchar(20),
  component_id_format varchar(10),
  component_id_coordinates_json varchar(1000), -- the component identifier coordinates (that caused the policy violation) stored in json format
  constraint_facts_json CLOB NOT NULL, -- the constraint facts (that caused the policy violation) stored in json format
  action_type_id varchar(20),
  notifications CLOB, -- email addresses notified for this policy violation, delimited by new lines
  waived bool DEFAULT false NOT NULL,
  latest_evaluation bool DEFAULT true NOT NULL, -- Whether this violation is from the latest evaluation,
  CONSTRAINT repository_policy_violation_pk PRIMARY KEY (repository_policy_violation_id),
  CONSTRAINT repository_policy_violation_repository_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id)
);
CREATE INDEX repository_policy_violation_hash_idx ON repository_policy_violation(hash);
