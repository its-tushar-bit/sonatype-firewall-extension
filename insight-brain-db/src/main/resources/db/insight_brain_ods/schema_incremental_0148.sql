-- Since 1.79.0

-- create the root source control entry if it doesn't already exist AND there are non-root source control entries
INSERT INTO source_control (source_control_id, owner_id)
SELECT 'efefd2dc4a24494689d51999b384b560', 'ROOT_ORGANIZATION_ID'
WHERE NOT EXISTS (
        SELECT owner_id FROM source_control WHERE owner_id='ROOT_ORGANIZATION_ID'
    )
  AND EXISTS (
        SELECT * FROM source_control
    )
LIMIT 1;

-- update the root source control provider to the most popular one IFF it's not already set
UPDATE source_control
SET provider = (
    SELECT provider FROM (
                             SELECT COUNT(*) AS popularity, provider
                             FROM (
                                      SELECT UPPER(provider) AS provider
                                      FROM source_control
                                      WHERE provider IS NOT NULL AND trim(provider) != ''
                                  ) AS providers
                             GROUP BY provider
                             ORDER BY popularity DESC, provider ASC
                             LIMIT 1
                         ) AS most_popular_provider
)
WHERE owner_id = 'ROOT_ORGANIZATION_ID';

-- set the new default settings on the root source control record if it exists
UPDATE source_control
SET base_branch = 'master', enable_pull_requests = true, enable_status_checks = true
WHERE owner_id = 'ROOT_ORGANIZATION_ID';

-- clear the provider field in every other source control record except root
UPDATE source_control
SET provider = NULL
WHERE owner_id != 'ROOT_ORGANIZATION_ID';
