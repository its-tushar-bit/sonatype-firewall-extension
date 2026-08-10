-- SaaS Compatible
-- CLM-43708 H2 variant: no-op / version marker. H2 did plain in-place renames in N
-- (schema_incremental_0475.h2.sql / 0476.h2.sql) with no compat views or _t base tables, so
-- there is nothing to collapse for H2. The paired schema_incremental_0480.cls migrator also
-- no-ops on H2. This file exists only to keep the schema version counter aligned with the
-- Postgres 0480 collapse.
SELECT 1; -- dummy statement as script can't be empty
