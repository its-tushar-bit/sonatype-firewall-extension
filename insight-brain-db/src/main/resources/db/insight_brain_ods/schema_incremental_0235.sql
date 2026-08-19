-- Since 1.125
CREATE TABLE quarantined_component_access (
  quarantined_component_access_id varchar(50) NOT NULL PRIMARY KEY,
  repository_component_id varchar(50) NOT NULL,
  generate_time timestamp NOT NULL
);
