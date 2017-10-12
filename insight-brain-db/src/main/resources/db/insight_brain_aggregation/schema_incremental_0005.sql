SET SCHEMA insight_brain_aggregation;

ALTER TABLE success_metrics_report ADD COLUMN include_latest_data BOOLEAN NOT NULL DEFAULT true;
