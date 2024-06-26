-- since 1.178
-- SaaS Compatible

ALTER TABLE tenant_metadata ADD COLUMN organization_id varchar(50);
ALTER TABLE tenant_metadata ADD COLUMN organization_name varchar(100);
