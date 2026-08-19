-- Since 1.168
-- SaaS Compatible

ALTER TABLE saml_group DROP CONSTRAINT saml_group_name_uk;
ALTER TABLE saml_group ALTER COLUMN name TYPE text;
