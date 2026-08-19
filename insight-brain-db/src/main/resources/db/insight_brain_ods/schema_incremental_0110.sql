-- Since 1.33.0
CREATE TABLE system_configuration_property (
  system_configuration_property_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL,
  value varchar(500) NOT NULL,
  CONSTRAINT system_configuration_property_pk PRIMARY KEY (system_configuration_property_id),
  CONSTRAINT system_configuration_property_name_uk UNIQUE KEY (name)
);
-- Add  default value for SUCCESS_METRICS_ENABLED (true)
INSERT INTO system_configuration_property (system_configuration_property_id, name, value) VALUES ('39ae05576fcf474da6771b9f879759f7', 'SUCCESS_METRICS_ENABLED', 'true');
