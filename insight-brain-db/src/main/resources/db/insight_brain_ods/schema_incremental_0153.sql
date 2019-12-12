-- Since 1.81.0
ALTER TABLE policy_violation ALTER COLUMN component_id_format TYPE varchar(50);
ALTER TABLE application_component ALTER COLUMN component_id_format TYPE varchar(50);
