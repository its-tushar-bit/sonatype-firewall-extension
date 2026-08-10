-- SaaS Compatible
-- CLM-43708: Release N+1 collapse of the owner-widening compat views established in N
-- (CLM-42785 LC in 0475, CLM-42788 Firewall in 0476). Pattern A views are dropped here now that
-- no draining old pods remain; IF EXISTS keeps this safe on a tenant provisioned fresh during N
-- (which never had the view). Pattern B (relkind-guarded drop-view + rename of the _t base table)
-- runs in the paired schema_incremental_0480.cls PostIncrementalMigrator instead: the migrator
-- centralizes the postgres-only dialect guard (H2 renamed in place in N, so nothing to collapse) and
-- generates the six near-identical relkind-guarded DO blocks from one table list.

DROP VIEW IF EXISTS application_component;
DROP VIEW IF EXISTS application_component_license;
DROP VIEW IF EXISTS repository_component;
DROP VIEW IF EXISTS repository_policy_violation;
