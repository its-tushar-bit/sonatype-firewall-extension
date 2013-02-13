SET SCHEMA insight_brain_ods;

ALTER TABLE application
  DROP CONSTRAINT application_uk;

ALTER TABLE application
  ADD CONSTRAINT application_uk UNIQUE KEY (public_id_lowercase);
