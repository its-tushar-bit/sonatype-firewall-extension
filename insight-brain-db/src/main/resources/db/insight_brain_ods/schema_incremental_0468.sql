-- SaaS Compatible
-- CLM-40039: Remove legacy HostedRepositoryMonitoringTask Quartz job on upgrade.
-- The continuous monitoring flow now runs via RepositoryEvaluationQueueProducerJob
-- on the unified queue infrastructure. Without this cleanup, Quartz would try to
-- instantiate the deleted class and throw ClassNotFoundException on every scheduled
-- fire after upgrade.

DELETE FROM QRTZ_CRON_TRIGGERS WHERE TRIGGER_NAME = 'HostedRepositoryMonitoringTask';
DELETE FROM QRTZ_TRIGGERS WHERE TRIGGER_NAME = 'HostedRepositoryMonitoringTask';
DELETE FROM QRTZ_JOB_DETAILS WHERE JOB_NAME = 'HostedRepositoryMonitoringTask';
