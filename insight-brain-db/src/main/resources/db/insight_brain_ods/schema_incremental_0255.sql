-- Since 1.136
CREATE TABLE artifactory_connection (
  artifactory_connection_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  base_url varchar(2048) NOT NULL,
  username varchar(255),
  password varchar(255),
  CONSTRAINT artifactory_connection_pk PRIMARY KEY (artifactory_connection_id)
);
