-- since 1.183
-- SaaS Compatible
CREATE TABLE IF NOT EXISTS  auto_policy_waiver (
    auto_policy_waiver_id VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50) NOT NULL,
    threat_level SMALLINT NOT NULL,
    reachable BOOLEAN,
    path_forward BOOLEAN,
    creator_id VARCHAR(60) NOT NULL,
    creator_name varchar(210) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    CONSTRAINT auto_policy_waiver_pk
    PRIMARY KEY (auto_policy_waiver_id)
    );

ALTER TABLE policy_violation ADD COLUMN auto_policy_waiver_id VARCHAR(50) NULL;

CREATE INDEX auto_policy_waiver_owner_id_idx ON auto_policy_waiver(owner_id);