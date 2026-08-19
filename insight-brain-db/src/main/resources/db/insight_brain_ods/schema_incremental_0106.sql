-- Since 1.25.0
CREATE TABLE webhook (
  webhook_id varchar(50) NOT NULL,
  url varchar(2048) NOT NULL,
  secret_key varchar(512),
  CONSTRAINT webhook_pk PRIMARY KEY (webhook_id)
);

CREATE TABLE webhook_event_type (
  webhook_id  varchar(50) NOT NULL,
  event_type varchar(50) NOT NULL,
  CONSTRAINT webhook_event_type_pk PRIMARY KEY (webhook_id, event_type),
  CONSTRAINT webhook_event_type_fk FOREIGN KEY (webhook_id) REFERENCES webhook(webhook_id)
);
