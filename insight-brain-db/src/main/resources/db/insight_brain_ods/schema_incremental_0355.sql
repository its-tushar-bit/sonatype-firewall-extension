-- Since 1.183
-- SaaS Compatible

CREATE TABLE IF NOT EXISTS auto_policy_waiver_revocation (
    auto_policy_waiver_revocation_id VARCHAR(50) NOT NULL,
    owner_id VARCHAR(50) NOT NULL,
    creator_id VARCHAR(60) NOT NULL,
    creator_name varchar(210) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    auto_policy_waiver_id varchar(50) NOT NULL,
    hash varchar(20) NOT NULL,
    associated_package_url varchar(1000) NULL,
    scan_id varchar(50) NOT NULL,
    CONSTRAINT auto_policy_waiver_revocation_pk
    PRIMARY KEY (auto_policy_waiver_revocation_id),
    CONSTRAINT auto_policy_waiver_revocation_auto_waiver_fk 
    FOREIGN KEY (auto_policy_waiver_id) REFERENCES auto_policy_waiver(auto_policy_waiver_id)
);

CREATE INDEX auto_policy_waiver_revocation_owner_id_idx on auto_policy_waiver_revocation(owner_id);
