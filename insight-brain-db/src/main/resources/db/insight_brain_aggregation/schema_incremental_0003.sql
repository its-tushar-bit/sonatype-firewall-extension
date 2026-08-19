CREATE TABLE success_metrics (
  success_metrics_id VARCHAR(50) NOT NULL,
  username VARCHAR(60) NOT NULL, -- The internal name of the User (CLM User or LDAP user)
  name VARCHAR(60) NOT NULL,
  name_lowercase_no_whitespace VARCHAR(60) NOT NULL,
  scope_json CLOB NOT NULL, -- The scope (app/org ids) stored in json format
  create_time DATETIME NOT NULL,
  CONSTRAINT success_metrics_pk PRIMARY KEY (success_metrics_id),
  CONSTRAINT success_metrics_uk UNIQUE KEY (username, name_lowercase_no_whitespace)
);
