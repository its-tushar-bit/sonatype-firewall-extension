-- since 1.191
-- SaaS Compatible
CREATE TABLE zscaler_configuration
(
    zscaler_configuration_id varchar(50)  NOT NULL,
    hostname                 varchar(255) NOT NULL,
    apikey                   varchar(255) NOT NULL,
    username                 varchar(255),
    password                 varchar(255),
    CONSTRAINT zscaler_configuration_pk PRIMARY KEY (zscaler_configuration_id)
);