-- Since 1.15.0
SET SCHEMA insight_brain_ods;

-- add new columns
ALTER TABLE role
  ADD COLUMN sort_order int NULL;

UPDATE role SET sort_order='100' WHERE role_id='1b92fae3e55a411793a091fb821c422d';
UPDATE role SET sort_order='200' WHERE role_id='1cddabf7fdaa47d6833454af10e0a3ef';
UPDATE role SET sort_order='300' WHERE role_id='1da70fae1fd54d6cb7999871ebdb9a36';
UPDATE role SET sort_order='400' WHERE role_id='2cb71b3468d649789163ea2e212b541e';
UPDATE role SET sort_order='500' WHERE role_id='90c7c98683b4471cb77a916744540bcc';

ALTER TABLE role
  ALTER COLUMN sort_order int NOT NULL;
