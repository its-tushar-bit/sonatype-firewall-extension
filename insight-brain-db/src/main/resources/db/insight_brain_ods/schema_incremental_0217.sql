-- Since tbd
UPDATE source_control_event
SET event_type = 'source control evaluation'
WHERE event_type = 'manifest evaluation';
