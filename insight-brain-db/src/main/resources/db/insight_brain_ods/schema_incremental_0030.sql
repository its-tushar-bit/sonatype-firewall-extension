SET SCHEMA insight_brain_ods;

UPDATE user SET email='admin@localhost' WHERE user_id='ADMIN';

ALTER TABLE user
  ALTER COLUMN password varchar(128) NOT NULL;
ALTER TABLE user
  ALTER COLUMN email varchar(255) NOT NULL;
