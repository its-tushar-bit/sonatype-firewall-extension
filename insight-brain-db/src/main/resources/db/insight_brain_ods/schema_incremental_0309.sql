-- Since 1.167
-- SaaS Compatible

-- Adding default value as the current date for the new column `created` so we ensure the new code will handle deletion
-- for any existing tenant on the deleted_tenant table. We are setting the default value to CURRENT_TIMESTAMP instead
-- of using the `delete_requested_timestamp` column value because we didn't have a proper function that is compatible
-- with both H2 and Postgres DBs to transform from a long to timestamp.
ALTER TABLE deleted_tenant ADD created timestamp default CURRENT_TIMESTAMP(6);
ALTER TABLE deleted_tenant ADD last_updated timestamp default NULL;
ALTER TABLE deleted_tenant ADD delete_completed_date timestamp default NULL;
ALTER TABLE deleted_tenant ALTER COLUMN delete_requested_timestamp DROP NOT NULL;
