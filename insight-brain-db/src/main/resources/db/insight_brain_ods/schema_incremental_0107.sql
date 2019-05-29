-- Since 1.29.0
CREATE TABLE system_notice (
  system_notice_id varchar(50) NOT NULL,
  message varchar(500) NOT NULL,
  enabled boolean NOT NULL,
  CONSTRAINT system_notice_pk PRIMARY KEY (system_notice_id)
);
-- Add  default system notice
INSERT INTO system_notice (system_notice_id, message, enabled) VALUES ('system-notice', '', false);
