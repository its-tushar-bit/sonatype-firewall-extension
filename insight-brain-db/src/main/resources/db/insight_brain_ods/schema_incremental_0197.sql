-- Since 1.100

CREATE TABLE perpetual_lock (
  perpetual_lock_id VARCHAR(1100) NOT NULL,
  owner VARCHAR(50),
  expiration_time timestamp,
  CONSTRAINT perpetual_lock_id_pk PRIMARY KEY (perpetual_lock_id)
);
