-- Since 1.186
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS scm_user_mappings (
    scm_user_mappings_id VARCHAR(50) NOT NULL,
    role_id VARCHAR(50) NULL,
    organization_id VARCHAR(50) NOT NULL,
    mappings_json TEXT NOT NULL,
    CONSTRAINT scm_user_mappings_pk PRIMARY KEY (scm_user_mappings_id),
    CONSTRAINT scm_user_mappings_organization_uk UNIQUE (organization_id)
    );
