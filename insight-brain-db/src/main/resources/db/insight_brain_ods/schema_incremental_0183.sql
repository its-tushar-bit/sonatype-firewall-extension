-- since 1.98
ALTER TABLE source_control_event
ADD COLUMN event_priority int NOT NULL DEFAULT 2;

ALTER TABLE source_control_event
ADD COLUMN policy_evaluation_outcome varchar(20);

ALTER TABLE source_control_event
ADD COLUMN critical_component_count int NOT NULL DEFAULT 0;

ALTER TABLE source_control_event
ADD COLUMN severe_component_count int NOT NULL DEFAULT 0;

ALTER TABLE source_control_event
ADD COLUMN moderate_component_count int NOT NULL DEFAULT 0;
