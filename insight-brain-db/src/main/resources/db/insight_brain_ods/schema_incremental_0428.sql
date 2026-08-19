-- SaaS Compatible
CREATE TABLE IF NOT EXISTS version_evaluation_window (
  version_evaluation_window_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  context_id varchar(30) NOT NULL,
  max_versions smallint,
  max_age_in_days smallint,
  CONSTRAINT version_evaluation_window_pk PRIMARY KEY (version_evaluation_window_id),
  CONSTRAINT version_evaluation_window_uk UNIQUE (owner_id, context_id)
);
