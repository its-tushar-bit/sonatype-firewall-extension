-- SaaS Compatible
-- NEXUS-52487: Add waiver_expiration_notification_config table for configurable waiver expiration notifications

CREATE TABLE IF NOT EXISTS waiver_expiration_notification_config (
  waiver_expiration_notification_config_id varchar(50) NOT NULL,
  owner_id varchar(50) NOT NULL,
  notification_days varchar(20),
  notifications_json TEXT,
  CONSTRAINT waiver_expiration_notification_config_pk PRIMARY KEY (waiver_expiration_notification_config_id),
  CONSTRAINT waiver_expiration_notification_config_owner_uk UNIQUE (owner_id)
);
