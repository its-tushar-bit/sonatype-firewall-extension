-- Since 1.159
-- Temporary table for firewall repository managers onboarding requests
CREATE TABLE firewall_onboarding_repository_manager (
  firewall_onboarding_repository_manager_id varchar(50) NOT NULL,
  instance_id varchar(50) NOT NULL,
  request_time timestamp NOT NULL,
  request_username varchar(60) NOT NULL,
  configure_time timestamp,
  configure_username varchar(60),
  request_user_agent varchar(300),
  CONSTRAINT firewall_onboarding_repository_manager_pk PRIMARY KEY (firewall_onboarding_repository_manager_id),
  CONSTRAINT firewall_onboarding_repository_manager_uk UNIQUE (instance_id)
);

-- Temporary table for repositories for firewall repository managers onboarding requests
CREATE TABLE firewall_onboarding_repository (
  firewall_onboarding_repository_id varchar(50) NOT NULL,
  firewall_onboarding_repository_manager_id varchar(50) NOT NULL,
  name varchar(500) NOT NULL,
  format varchar(50) NOT NULL,
  -- proxy or hosted
  type varchar(10) NOT NULL,
  audit_enabled bool DEFAULT false NOT NULL,
  quarantine_enabled bool DEFAULT false NOT NULL,
  namespace_confusion_protection_enabled bool DEFAULT false NOT NULL,
  CONSTRAINT firewall_onboarding_repository_pk PRIMARY KEY (firewall_onboarding_repository_id),
  CONSTRAINT firewall_onboarding_repository_uk UNIQUE (firewall_onboarding_repository_manager_id, name),
  CONSTRAINT firewall_onboarding_repository_repository_manager_fk
    FOREIGN KEY (firewall_onboarding_repository_manager_id)
    REFERENCES firewall_onboarding_repository_manager(firewall_onboarding_repository_manager_id)
);