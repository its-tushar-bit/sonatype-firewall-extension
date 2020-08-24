-- since 1.98

-- we are now rolling out polling for GitLab MRs but customers using GitLab already with source control configured
-- most likely already have polling errors accumulated.  This will result in delays in processing existing merge
-- requests for commenting.  Therefore, we will clear out the errors and reset the polling times IFF the configured
-- source control provider is GitLab
UPDATE source_control
SET pull_request_error_count = 0, pull_request_poll_time = CURRENT_TIMESTAMP
WHERE source_control_id IN (
  SELECT source_control_id FROM (
    SELECT sc.source_control_id, scm.provider, sc.pull_request_error_count
    FROM source_control scm, source_control sc
    WHERE scm.owner_id = 'ROOT_ORGANIZATION_ID'
      AND scm.provider = 'GITLAB'
      AND sc.pull_request_error_count > 0
  ) gitlab_errors
);
