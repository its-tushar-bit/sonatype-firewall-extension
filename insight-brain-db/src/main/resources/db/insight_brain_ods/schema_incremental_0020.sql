SET SCHEMA insight_brain_ods;

ALTER TABLE hash_gav
  DROP CONSTRAINT hash_gav_id;
ALTER TABLE hash_gav
  ADD CONSTRAINT hash_gav_pk PRIMARY KEY (hash_gav_id);
