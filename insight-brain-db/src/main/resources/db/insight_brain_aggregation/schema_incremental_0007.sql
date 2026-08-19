-- Delete all aggregation data.
-- It will be regenerated on demand and it will include values for the new column added below.
DELETE FROM policy_violation_aggregation;
DELETE FROM policy_violation_resolution_state;
DELETE FROM success_metrics_report_data;

ALTER TABLE policy_violation_resolution_state ADD COLUMN constraint_facts_json CLOB NOT NULL;
