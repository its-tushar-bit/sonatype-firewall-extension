-- Since 1.11
SET SCHEMA insight_brain_ods;

CREATE TABLE dashboard_filter (
  dashboard_filter_id varchar(50) NOT NULL,
  username varchar(60) NOT NULL, -- The internal name of the User (CLM User or LDAP user)
  filter_json CLOB NOT NULL, -- The dashboard filter stored in json format
  CONSTRAINT dashboard_filter_pk PRIMARY KEY (dashboard_filter_id)
);
