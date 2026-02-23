-- SaaS Compatible
CREATE TABLE IF NOT EXISTS key_value (
  key varchar(50) NOT NULL,
  value varchar(4000) NOT NULL,
  CONSTRAINT key_value_pk PRIMARY KEY (key)
);
