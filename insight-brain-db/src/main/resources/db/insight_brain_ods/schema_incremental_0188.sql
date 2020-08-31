-- Since 1.98
CREATE TABLE persisted_promote_scan_result (
  persisted_promote_scan_result_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  status varchar(50) NOT NULL,
  scan_id varchar(50) NULL,
  error_message varchar(1000) NULL,
  create_time timestamp NOT NULL,
  CONSTRAINT persisted_promote_scan_result_pk PRIMARY KEY (persisted_promote_scan_result_id)
);
CREATE INDEX persisted_promote_scan_result_create_time_idx ON persisted_promote_scan_result(create_time);
