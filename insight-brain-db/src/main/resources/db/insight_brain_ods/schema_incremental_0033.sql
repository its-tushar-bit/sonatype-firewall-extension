SET SCHEMA insight_brain_ods;

ALTER TABLE application
  ADD COLUMN contact_internal_name varchar(60) NULL;  -- The internal name of the contact User (CLM User or LDAP user)
