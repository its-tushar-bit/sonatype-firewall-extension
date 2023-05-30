-- Since 1.162
CREATE TABLE IF NOT EXISTS tenant_metadata
(
  tenant_metadata_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  application_name varchar(100) NOT NULL,
  connection_id varchar(50) NOT NULL,
  connection_name varchar(100) NOT NULL,
  CONSTRAINT tenant_metadata_pk PRIMARY KEY (tenant_metadata_id)
);
