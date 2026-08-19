-- since 1.184

-- SaaS Compatible
CREATE INDEX firewall_metrics_name_date_idx ON firewall_metrics(metrics_name, metrics_date);
