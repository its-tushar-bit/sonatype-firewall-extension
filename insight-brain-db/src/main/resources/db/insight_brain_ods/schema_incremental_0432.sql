-- SaaS Compatible
CREATE TABLE IF NOT EXISTS evaluation_queue (
  create_time timestamp NOT NULL,
  update_time timestamp NOT NULL,
  priority int NOT NULL,
  evaluation_queue_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  stage_type_id varchar(30) NOT NULL,
  version varchar(1100) NOT NULL,
  worker_id varchar(50),
  CONSTRAINT evaluation_queue_pk PRIMARY KEY (evaluation_queue_id),
  CONSTRAINT evaluation_queue_uk UNIQUE (application_id, stage_type_id, version),
  CONSTRAINT evaluation_queue_app_fk FOREIGN KEY (application_id) REFERENCES application(application_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS evaluation_queue_priority_idx ON evaluation_queue(priority);
