SET SCHEMA insight_brain_aggregation;

ALTER TABLE policy_violation_aggregation ADD COLUMN time_period_end DATETIME NULL;
