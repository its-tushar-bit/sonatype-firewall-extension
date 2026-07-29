-- SaaS Compatible
-- CLM-42780: Remove Quartz rows for deleted HostedDeploymentBlockCleanupTask.
-- The sync-enforcement feature (CLM-39706) was cancelled; the job class no longer exists.
-- Without this cleanup, the scheduler throws ClassNotFoundException on startup.

DELETE FROM QRTZ_SIMPLE_TRIGGERS  WHERE TRIGGER_NAME = 'HostedDeploymentBlockCleanupTask';
DELETE FROM QRTZ_CRON_TRIGGERS    WHERE TRIGGER_NAME = 'HostedDeploymentBlockCleanupTask';
DELETE FROM QRTZ_BLOB_TRIGGERS    WHERE TRIGGER_NAME = 'HostedDeploymentBlockCleanupTask';
DELETE FROM QRTZ_SIMPROP_TRIGGERS WHERE TRIGGER_NAME = 'HostedDeploymentBlockCleanupTask';
DELETE FROM QRTZ_FIRED_TRIGGERS   WHERE JOB_NAME     = 'HostedDeploymentBlockCleanupTask';
DELETE FROM QRTZ_TRIGGERS         WHERE JOB_NAME     = 'HostedDeploymentBlockCleanupTask';
DELETE FROM QRTZ_JOB_DETAILS      WHERE JOB_NAME     = 'HostedDeploymentBlockCleanupTask';
