-- since 1.201
-- SaaS Compatible

-- owner_id can be an application or organization id
-- owner_type can be 'APPLICATION' or 'ORGANIZATION'
CREATE TABLE IF NOT EXISTS ci_integrations_config (
    ci_integrations_config_id VARCHAR(50) NOT NULL,
    configuration_json TEXT,
    owner_id VARCHAR(50) NOT NULL,
    owner_type VARCHAR(20) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    CONSTRAINT ci_integrations_config_pk PRIMARY KEY (ci_integrations_config_id),
    CONSTRAINT ci_integrations_config_owner_uk UNIQUE (owner_id, owner_type)
);