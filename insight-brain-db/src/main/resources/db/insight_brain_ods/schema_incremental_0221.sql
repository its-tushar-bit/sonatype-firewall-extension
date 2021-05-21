-- Since 1.115
ALTER TABLE source_control_event DROP CONSTRAINT source_control_event_application_fk;

DROP INDEX source_control_event_application_id_idx;

ALTER TABLE source_control_event ADD CONSTRAINT source_control_event_application_fk FOREIGN KEY (application_id) REFERENCES application (application_id);

