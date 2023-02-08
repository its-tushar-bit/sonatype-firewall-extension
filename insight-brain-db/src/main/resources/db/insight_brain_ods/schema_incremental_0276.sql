-- Since 1.154
UPDATE qrtz_job_details
SET job_class_name = 'com.sonatype.insight.brain.git.PullRequestMonitor'
WHERE sched_name = 'QuartzScheduler'
  AND job_name = 'PullRequestMonitor';

