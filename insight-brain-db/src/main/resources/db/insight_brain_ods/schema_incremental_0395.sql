-- SaaS Compatible
-- Since 1.193
CREATE TABLE zscaler_format (
    zscaler_format_id varchar(50) NOT NULL,
    zscaler_configuration_id varchar(50) NOT NULL,
    format VARCHAR(50) NOT NULL,
    enabled bool NOT NULL,
    CONSTRAINT zscaler_format_pk PRIMARY KEY (zscaler_format_id),
    CONSTRAINT zscaler_format_uk UNIQUE (format),
    CONSTRAINT zscaler_format_configuration_fk FOREIGN KEY (zscaler_configuration_id)
        REFERENCES zscaler_configuration(zscaler_configuration_id) ON DELETE CASCADE
);
