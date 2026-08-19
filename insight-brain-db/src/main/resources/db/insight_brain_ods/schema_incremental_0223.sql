-- Since 1.116

DELETE FROM qrtz_simple_triggers
WHERE trigger_name = 'PullRequestDetailsUpdater';

DELETE FROM qrtz_triggers
WHERE trigger_name = 'PullRequestDetailsUpdater';

DELETE FROM qrtz_job_details
WHERE job_name = 'PullRequestDetailsUpdater';
