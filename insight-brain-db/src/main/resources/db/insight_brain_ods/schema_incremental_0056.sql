-- Since 1.11
SET SCHEMA insight_brain_ods;
ALTER TABLE ldap_usermapping
  ADD COLUMN dynamic_group_search_enabled boolean DEFAULT true NOT NULL;