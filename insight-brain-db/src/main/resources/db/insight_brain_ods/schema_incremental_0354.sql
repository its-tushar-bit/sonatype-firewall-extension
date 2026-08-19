-- Since 1.183
-- SaaS Compatible

DELETE FROM qrtz_blob_triggers WHERE trigger_name = 'PolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_cron_triggers WHERE trigger_name = 'PolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_simple_triggers WHERE trigger_name = 'PolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_simprop_triggers WHERE trigger_name = 'PolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_triggers WHERE trigger_name = 'PolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_fired_triggers WHERE trigger_name = 'PolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_job_details WHERE job_name = 'PolicyViolationConstraintFactsJsonAsyncDbMigration';

DELETE FROM qrtz_blob_triggers WHERE trigger_name = 'RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_cron_triggers WHERE trigger_name = 'RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_simple_triggers WHERE trigger_name = 'RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_simprop_triggers WHERE trigger_name = 'RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_triggers WHERE trigger_name = 'RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_fired_triggers WHERE trigger_name = 'RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration';
DELETE FROM qrtz_job_details WHERE job_name = 'RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration';
