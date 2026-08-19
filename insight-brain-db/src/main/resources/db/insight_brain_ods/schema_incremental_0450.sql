-- Since 1.204
-- SaaS Compatible
-- CLM-38070: Create consumption_events (VARCHAR(50) UUID PK) and consumption_limit_config tables
-- CLM-39593: Add Usage Viewer role for consumption dashboard access

CREATE TABLE IF NOT EXISTS consumption_events (
                                                  id VARCHAR(50) NOT NULL,
    org_id VARCHAR(255) NOT NULL,
    app_id VARCHAR(255),
    scan_id VARCHAR(255),
    user_id VARCHAR(255),
    event_timestamp TIMESTAMP NOT NULL,
    component_count INTEGER NOT NULL DEFAULT 1,
    activity_type VARCHAR(50) NOT NULL,
    source VARCHAR(30) NOT NULL,
    tier VARCHAR(20) NOT NULL,
    billing_month DATE NOT NULL,
    CONSTRAINT consumption_events_pk PRIMARY KEY (id),
    CONSTRAINT chk_consumption_events_component_count CHECK (component_count > 0)
    );

CREATE INDEX IF NOT EXISTS idx_consumption_events_org_month ON consumption_events(org_id, billing_month);
CREATE INDEX IF NOT EXISTS idx_consumption_events_org_activity ON consumption_events(org_id, activity_type, billing_month);
CREATE INDEX IF NOT EXISTS idx_consumption_events_timestamp ON consumption_events(event_timestamp);
CREATE INDEX IF NOT EXISTS idx_consumption_events_billing_activity ON consumption_events(billing_month, activity_type);

CREATE TABLE IF NOT EXISTS consumption_limit_config (
                                                        id VARCHAR(50) NOT NULL,
    org_id VARCHAR(255) NOT NULL,
    monthly_limit BIGINT,
    warning_threshold_pct INTEGER DEFAULT 80,
    enforcement_mode VARCHAR(10) DEFAULT 'SOFT',
    CONSTRAINT consumption_limit_config_pk PRIMARY KEY (id),
    CONSTRAINT consumption_limit_config_org_id_uk UNIQUE (org_id),
    CONSTRAINT chk_warning_threshold_pct CHECK (warning_threshold_pct BETWEEN 0 AND 100),
    CONSTRAINT chk_enforcement_mode CHECK (enforcement_mode IN ('SOFT', 'HARD'))
    );

-- CLM-39593: Usage Viewer role and VIEW_USAGE permission
INSERT INTO role (role_id, name, name_lowercase_no_whitespace, sort_order, description, global, built_in)
SELECT '070e6c31fc8a42159df5298313b8a829', 'Usage Viewer', 'usageviewer', 700, 'Can view the Usage Dashboard, consumption API, and export data', TRUE, TRUE
    WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_id = '070e6c31fc8a42159df5298313b8a829');

INSERT INTO role_permission (role_permission_id, role_id, permission)
SELECT 'd95edf65c9ec4b25b25f6f0f0ab998ff', '1b92fae3e55a411793a091fb821c422d', 'VIEW_USAGE'
    WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = '1b92fae3e55a411793a091fb821c422d' AND permission = 'VIEW_USAGE');

INSERT INTO role_permission (role_permission_id, role_id, permission)
SELECT '8083539846424d66b6dbe1ac0cbbc0c7', '070e6c31fc8a42159df5298313b8a829', 'VIEW_USAGE'
    WHERE NOT EXISTS (SELECT 1 FROM role_permission WHERE role_id = '070e6c31fc8a42159df5298313b8a829' AND permission = 'VIEW_USAGE');

CREATE INDEX IF NOT EXISTS idx_consumption_events_timestamp_app
    ON consumption_events(event_timestamp, app_id);
