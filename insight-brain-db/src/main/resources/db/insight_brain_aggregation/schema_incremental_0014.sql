-- since 1.169
-- From this point forward all schema changes must be SaaS compatible and must include the following statement:

-- SaaS Compatible

-- See this internal document for more details on what this means and how to craft SaaS compatible schema changes:
-- https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368/SaaS+Friendly+IQ+Database+Migrations

CREATE TABLE firewall_metrics (
    firewall_metrics_id varchar(50) NOT NULL,
    metrics_date date NOT NULL,
    metrics_name varchar(255) NOT NULL,
    metrics_value int NOT NULL,
    metrics_last_updated_at timestamp NOT NULL,

    CONSTRAINT firewall_metrics_id_pk PRIMARY KEY (firewall_metrics_id)
);
