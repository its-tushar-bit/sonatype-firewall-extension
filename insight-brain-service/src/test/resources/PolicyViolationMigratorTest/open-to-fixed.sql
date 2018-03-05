SET SCHEMA insight_brain_ods;

INSERT INTO organization (organization_id, parent_organization_id, name, name_lowercase_no_whitespace)
  VALUES ('org-0', 'ROOT_ORGANIZATION_ID', 'Organization 0', 'organization0');
INSERT INTO application (application_id, public_id, public_id_lowercase, name, name_lowercase_no_whitespace, organization_id) 
  VALUES ('app-0', 'App-0', 'app-0', 'Application 0', 'application0', 'org-0');

INSERT INTO policy_evaluation (policy_evaluation_id, application_id, stage_type_id, scan_id, reevaluation, for_monitoring, for_obsolete_scan, time)
  VALUES ('eval-0', 'app-0', 'build', 'scan-0', false, false, false, '2018-02-01 01:23:45');

INSERT INTO policy_violation (policy_violation_id, policy_evaluation_id, time, waived, policy_id, policy_name, threat_level, threat_category, hash, component_id_format, component_id_coordinates_json, pathnames, action_type_id, constraint_facts_json)
  VALUES ('eval-0-vio-0', 'eval-0', '2018-02-01 01:23:45', false, 'policy-0', 'Policy 0', 5, 'security', 'hash-0', 'npm', '{"packageId":"test","version":"1.0"}', 'test-1.0.tgz', 'fail', 'constraints-0');

INSERT INTO policy_evaluation (policy_evaluation_id, application_id, stage_type_id, scan_id, reevaluation, for_monitoring, for_obsolete_scan, time)
  VALUES ('eval-1', 'app-0', 'build', 'scan-1', true, false, false, '2018-02-02 01:23:45');
