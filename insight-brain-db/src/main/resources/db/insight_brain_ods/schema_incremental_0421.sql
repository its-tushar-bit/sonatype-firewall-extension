-- Since 1.197
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS enterprise_reporting_filter (
  enterprise_reporting_filter_id varchar(50) NOT NULL,
  filter_name varchar(60) NOT NULL,
  filter_json text NOT NULL,
  user_id varchar(50) NOT NULL,
  CONSTRAINT enterprise_reporting_filter_uk UNIQUE (filter_name, user_id),
  CONSTRAINT enterprise_reporting_filter_pk PRIMARY KEY (enterprise_reporting_filter_id),
  CONSTRAINT enterprise_reporting_filter_user_fk FOREIGN KEY (user_id) REFERENCES "user"(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS enterprise_reporting_default_filter (
  enterprise_reporting_filter_id varchar(50) NOT NULL,
  user_id varchar(50) NOT NULL,
  CONSTRAINT enterprise_reporting_default_filter_user_pk PRIMARY KEY (user_id),
  CONSTRAINT enterprise_reporting_default_filter_id_fk FOREIGN KEY (enterprise_reporting_filter_id)
      REFERENCES enterprise_reporting_filter(enterprise_reporting_filter_id) ON DELETE CASCADE,
  CONSTRAINT enterprise_reporting_default_filter_user_fk FOREIGN KEY (user_id)
      REFERENCES "user"(user_id) ON DELETE CASCADE
);