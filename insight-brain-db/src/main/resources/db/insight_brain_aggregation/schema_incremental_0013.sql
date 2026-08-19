-- since 1.163
-- From this point forward all schema changes must be SaaS compatible and must include the following statement:

-- SaaS Compatible

-- See this internal document for more details on what this means and how to craft SaaS compatible schema changes:
-- https://sonatype.atlassian.net/wiki/spaces/MTIQ/pages/36318368/SaaS+Friendly+IQ+Database+Migrations

-- since our tests don't allow empty db script files the following trivial SQL is included
SELECT COUNT(*) FROM schema_version;