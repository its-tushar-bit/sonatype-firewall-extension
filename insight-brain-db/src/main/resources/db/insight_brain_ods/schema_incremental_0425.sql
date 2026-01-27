-- Since 1.202
-- SaaS Compatible
-- CLM-35001: Update pull_request_monitoring_interval_seconds to meet new minimum of 60 seconds

UPDATE source_control_configuration
SET pull_request_monitoring_interval_seconds = 60
WHERE pull_request_monitoring_interval_seconds < 60;
