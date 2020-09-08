-- Since 1.98
CREATE TABLE persisted_scan_ticket (
  persisted_scan_ticket_id varchar(50) NOT NULL,
  application_id varchar(50) NOT NULL,
  scan_id varchar(50),
  state_id varchar(50) NOT NULL,
  error_id varchar(50),
  create_time timestamp NOT NULL,
  CONSTRAINT persisted_scan_ticket_pk PRIMARY KEY (persisted_scan_ticket_id)
);
CREATE INDEX persisted_scan_ticket_create_time_idx ON persisted_scan_ticket(create_time);
