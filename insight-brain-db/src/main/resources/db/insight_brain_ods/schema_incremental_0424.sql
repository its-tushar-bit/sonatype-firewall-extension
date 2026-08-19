-- Since 1.201
-- SaaS Compatible

-- owner_id can be an application or an organization id
CREATE TABLE IF NOT EXISTS github_app (
  github_app_id VARCHAR(50) NOT NULL,
  owner_id VARCHAR(50) NOT NULL,
  app_id INTEGER NOT NULL,
  slug VARCHAR(256),
  client_id VARCHAR(256) NOT NULL,
  client_secret VARCHAR(512) NOT NULL,
  private_key TEXT NOT NULL,
  installation_id BIGINT NOT NULL,
  CONSTRAINT github_app_pk PRIMARY KEY (github_app_id),
  CONSTRAINT github_app_owner_id_uk UNIQUE (owner_id),
  CONSTRAINT github_app_installation_id_uk UNIQUE (installation_id),
  CONSTRAINT github_app_app_id_uk UNIQUE (app_id)
);

ALTER TABLE source_control ADD COLUMN IF NOT EXISTS github_app_id VARCHAR(50);

ALTER TABLE source_control ADD COLUMN IF NOT EXISTS authentication_type VARCHAR(20) DEFAULT 'PAT';

ALTER TABLE source_control DROP CONSTRAINT IF EXISTS source_control_github_app_fk;
ALTER TABLE source_control ADD CONSTRAINT source_control_github_app_fk
  FOREIGN KEY (github_app_id) REFERENCES github_app(github_app_id);
