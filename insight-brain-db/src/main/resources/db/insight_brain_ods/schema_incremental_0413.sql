-- SaaS Compatible

CREATE INDEX policy_evaluation_reeval_time_idx ON policy_evaluation (reevaluation, stage_type_id, application_id, time DESC);
