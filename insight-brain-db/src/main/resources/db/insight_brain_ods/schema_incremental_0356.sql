-- since 1.184
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS  malware_defense_metrics (
    malware_defense_metrics_id VARCHAR(50) NOT NULL,
    format VARCHAR(50) NOT NULL,
    malicious_component_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT malware_defense_metrics_pk PRIMARY KEY (malware_defense_metrics_id),
    CONSTRAINT malware_defense_metrics_uk UNIQUE (format)
    );
