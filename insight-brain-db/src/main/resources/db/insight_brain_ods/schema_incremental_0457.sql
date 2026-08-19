-- SaaS Compatible
-- CLM-39658: Add relay_configuration table for the SCM webhook relay integration.
-- Singleton row keyed by relay_configuration_id. api_key and webhook_signing_secret are
-- stored as encrypted values (PasswordHandler) and webhook_url / customer_id are returned
-- by the relay at registration time.

CREATE TABLE IF NOT EXISTS relay_configuration (
  relay_configuration_id varchar(50) NOT NULL,
  api_key varchar(2000),
  webhook_url varchar(2000),
  webhook_signing_secret varchar(2000),
  customer_id varchar(255),
  registered_at timestamp,
  CONSTRAINT relay_configuration_pk PRIMARY KEY (relay_configuration_id)
);
