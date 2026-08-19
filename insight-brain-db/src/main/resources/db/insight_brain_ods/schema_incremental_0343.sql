-- Since 1.178
-- SaaS Compatible

ALTER TABLE development_prioritization_component_info ADD COLUMN source_status varchar(20) NULL;
ALTER TABLE development_prioritization_component_info ADD COLUMN build_status varchar(20) NULL;
ALTER TABLE development_prioritization_component_info ADD COLUMN stage_release_status varchar(20) NULL;
ALTER TABLE development_prioritization_component_info ADD COLUMN release_status varchar(20) NULL;
