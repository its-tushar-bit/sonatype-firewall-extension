-- since 1.98
ALTER TABLE source_control_event
ADD COLUMN status_id varchar(50);

ALTER TABLE source_control_event
ADD COLUMN user_agent varchar(255);
