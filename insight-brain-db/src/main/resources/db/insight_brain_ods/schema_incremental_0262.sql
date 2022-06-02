-- Since 1.139
CREATE TABLE jira_configuration (
  jira_configuration_id varchar(50) NOT NULL,
  url varchar(2048) NOT NULL,
  username varchar(255),
  password varchar(255),
  custom_fields_json varchar(8192),
  CONSTRAINT jira_configuration_pk PRIMARY KEY (jira_configuration_id)
);
