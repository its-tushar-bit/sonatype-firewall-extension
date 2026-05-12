-- For tests only
CREATE TABLE IF NOT EXISTS test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE component_category (
  component_category_id varchar(1000) NOT NULL,
  path varchar(1000) NOT NULL,
  CONSTRAINT component_category_pk PRIMARY KEY (component_category_id),
  CONSTRAINT component_category_path_uk UNIQUE (path)
);

CREATE TABLE license (
  license_id varchar(1000) NOT NULL,
  shortDisplayName varchar(1000) NOT NULL,
  longDisplayName varchar(1000) default NULL,
  CONSTRAINT license_pk PRIMARY KEY (license_id),
  CONSTRAINT license_shortDisplayName_uk UNIQUE (shortDisplayName)
);

CREATE TABLE multi_license (
  multi_license_id varchar(1000) NOT NULL,
  shortDisplayName varchar(1000) NOT NULL,
  longDisplayName varchar(1000) default NULL,
  CONSTRAINT multi_license_pk PRIMARY KEY (multi_license_id),
  CONSTRAINT multi_license_shortDisplayName_uk UNIQUE (shortDisplayName)
);

CREATE TABLE multi_license_license (
  multi_license_id varchar(1000) NOT NULL,
  license_id varchar(1000) NOT NULL,
  CONSTRAINT multi_license_license_pk PRIMARY KEY (multi_license_id, license_id),
  CONSTRAINT multi_license_license_multi_fk FOREIGN KEY (multi_license_id) REFERENCES multi_license(multi_license_id),
  CONSTRAINT multi_license_license_license_fk FOREIGN KEY (license_id) REFERENCES license(license_id)
);

CREATE TABLE IF NOT EXISTS schema_version (
  data_store_id varchar(32) NOT NULL,
  schema_version int NOT NULL
);
INSERT INTO schema_version (data_store_id , schema_version) VALUES ('insight_brain_dm', -1);

CREATE TABLE firewall_ignore_patterns
(
    firewall_ignore_patterns_id   varchar(50) NOT NULL,
    firewall_ignore_patterns_json text,
    CONSTRAINT firewall_ignore_patterns_pk PRIMARY KEY (firewall_ignore_patterns_id)
);
INSERT INTO firewall_ignore_patterns(firewall_ignore_patterns_id) VALUES ('firewall-ignore-patterns');

CREATE TABLE announcement_banner
(
    announcement_banner_id varchar(50)              NOT NULL,
    enabled                boolean                  NOT NULL DEFAULT false,
    window_id              varchar(200),
    display_from           timestamp with time zone,
    display_until          timestamp with time zone,
    message                text,
    severity               varchar(20)              NOT NULL DEFAULT 'info',
    updated_at             timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT announcement_banner_pk PRIMARY KEY (announcement_banner_id)
);
INSERT INTO announcement_banner(announcement_banner_id, enabled) VALUES ('announcement-banner', false);
