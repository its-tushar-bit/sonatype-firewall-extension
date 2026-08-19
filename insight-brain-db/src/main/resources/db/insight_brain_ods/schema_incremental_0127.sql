-- Since 1.63
CREATE INDEX policy_evaluation_app_monitoring_stage_idx ON policy_evaluation(application_id, for_monitoring, stage_type_id);
