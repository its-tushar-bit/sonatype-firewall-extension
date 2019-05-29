ALTER TABLE success_metrics RENAME TO success_metrics_report;
ALTER TABLE success_metrics_report ALTER COLUMN success_metrics_id RENAME TO success_metrics_report_id;
ALTER TABLE success_metrics_report DROP CONSTRAINT success_metrics_pk;
ALTER TABLE success_metrics_report DROP CONSTRAINT success_metrics_uk;
ALTER TABLE success_metrics_report ADD CONSTRAINT success_metrics_pk PRIMARY KEY (success_metrics_report_id);
ALTER TABLE success_metrics_report ADD CONSTRAINT success_metrics_uk UNIQUE KEY
    (username, name_lowercase_no_whitespace);
