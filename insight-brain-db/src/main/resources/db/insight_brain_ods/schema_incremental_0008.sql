SET SCHEMA insight_brain_ods;

ALTER TABLE application
  ADD CONSTRAINT application_name_uk UNIQUE KEY (name_lowercase_no_whitespace);

