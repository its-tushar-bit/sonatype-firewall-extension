-- Since 1.100

DELETE FROM qrtz_simple_triggers
WHERE trigger_name = 'SourceControlEventProcessing';

DELETE FROM qrtz_triggers
WHERE trigger_name = 'SourceControlEventProcessing';

DELETE FROM qrtz_job_details
WHERE job_name = 'SourceControlEventProcessing';

DELETE FROM qrtz_simple_triggers
WHERE trigger_name = 'PullRequestPolling';

DELETE FROM qrtz_triggers
WHERE trigger_name = 'PullRequestPolling';

DELETE FROM qrtz_job_details
WHERE job_name = 'PullRequestPolling';
