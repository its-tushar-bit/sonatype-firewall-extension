-- since 1.170
-- From this point forward all schema changes must be SaaS compatible and must include the following statement:

-- SaaS Compatible

-- See this internal document for more details on what this means and how to craft SaaS compatible schema changes:
-- https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368/SaaS+Friendly+IQ+Database+Migrations

ALTER TABLE firewall_metrics ADD CONSTRAINT firewall_metrics_date_name_uk UNIQUE (metrics_date, metrics_name);
