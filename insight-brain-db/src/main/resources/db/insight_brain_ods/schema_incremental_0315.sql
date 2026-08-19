-- Since 1.169
-- SaaS Compatible

-- See this internal document for more details on what this means and how to craft SaaS compatible schema changes:
-- https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368/SaaS+Friendly+IQ+Database+Migrations

CREATE TABLE application_count_history
(
  application_count_history_id   varchar(50) NOT NULL,
  application_count              integer     NOT NULL,
  updated_date                   timestamp   NOT NULL,
  CONSTRAINT application_count_history_pk PRIMARY KEY (application_count_history_id)
);

CREATE INDEX application_count_history_updated_date ON application_count_history (updated_date);

INSERT INTO application_count_history (application_count_history_id,
                                       application_count,
                                       updated_date)
SELECT 'initialization',
       (SELECT COUNT(*) FROM application),
       NOW();
