-- For tests only
CREATE TABLE test_table (
  test_table_id varchar(50) NOT NULL,
  name varchar(50) NOT NULL
);

CREATE TABLE app_label (
  app_label_id  varchar(50) NOT NULL,
  app_id varchar(50) NOT NULL,
  label varchar(50) NOT NULL,
  label_lowercase varchar(50) NOT NULL,
  color varchar(20) NULL,
  CONSTRAINT app_label_pk PRIMARY KEY (app_label_id),
  CONSTRAINT app_label_uk UNIQUE KEY (app_id,label_lowercase)
);

CREATE TABLE app_artifact_label (
  app_artifact_label_id  varchar(50) NOT NULL,
  app_id varchar(50) NOT NULL,
  sha1 char(20) NOT NULL,
  label varchar(50) NOT NULL,
  label_lowercase varchar(50) NOT NULL,
  active bool DEFAULT true NOT NULL,
  create_user_id varchar(254) NOT NULL,
  create_time datetime NOT NULL,
  delete_user_id varchar(254),
  delete_time datetime NOT NULL,
  CONSTRAINT app_artifact_label_pk PRIMARY KEY (app_artifact_label_id),
  CONSTRAINT app_artifact_label_uk UNIQUE KEY (app_id,sha1,label_lowercase,delete_time)
);
