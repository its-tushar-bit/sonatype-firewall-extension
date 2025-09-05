-- SaaS Compatible

CREATE TABLE IF NOT EXISTS reevaluate_cascade_request (
  reevaluate_cascade_request_id varchar(50) NOT NULL,
  component_reference_hash varchar(50) NOT NULL,
  created_at timestamp DEFAULT now() NOT NULL,
  created_by_username varchar(255) NOT NULL,
  owner_id varchar(50) NULL,
  CONSTRAINT reevaluate_cascade_request_pk PRIMARY KEY (reevaluate_cascade_request_id)
);

CREATE TABLE IF NOT EXISTS reevaluate_cascade_progress (
  reevaluate_cascade_progress_id varchar(50) NOT NULL,
  reevaluate_cascade_request_id varchar(50) NOT NULL,
  repository_id varchar(50) NOT NULL,
  repository_component_id varchar(50) NOT NULL,
  quarantined boolean NOT NULL DEFAULT FALSE,
  status varchar(20) NOT NULL,
  CONSTRAINT reevaluate_cascade_progress_pk PRIMARY KEY (reevaluate_cascade_progress_id),
  CONSTRAINT reevaluate_cascade_request_id_fk FOREIGN KEY (reevaluate_cascade_request_id) REFERENCES reevaluate_cascade_request(reevaluate_cascade_request_id) ON DELETE CASCADE,
  CONSTRAINT repository_id_fk FOREIGN KEY (repository_id) REFERENCES repository(repository_id) ON DELETE CASCADE,
  CONSTRAINT repository_component_id_fk FOREIGN KEY (repository_component_id) REFERENCES repository_component(repository_component_id) ON DELETE CASCADE
);
