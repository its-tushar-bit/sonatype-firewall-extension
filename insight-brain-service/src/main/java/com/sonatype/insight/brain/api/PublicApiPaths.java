/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

/**
 * @since 1.11.0
 */
public class PublicApiPaths
{
  public static final String BASE_PATH = "api";

  private static final String API_VERSION_PATH_V2 = BASE_PATH + "/v2/";

  private static final String EXPERIMENTAL_PATH = BASE_PATH + "/experimental/";

  public static final String APP_RESOURCE_PATH = API_VERSION_PATH_V2 + "applications";

  public static final String ORG_RESOURCE_PATH = API_VERSION_PATH_V2 + "organizations";

  public static final String POLICY_RESOURCE_PATH = API_VERSION_PATH_V2 + "policies";

  public static final String POLICY_EXPORT_RESOURCE_PATH =
      API_VERSION_PATH_V2 + "policy/{ownerType: application|organization|repository}/{ownerId}";

  public static final String CYCLONE_DX_RESOURCE_PATH = API_VERSION_PATH_V2 + "cycloneDx";

  public static final String SPDX_RESOURCE_PATH = API_VERSION_PATH_V2 + "spdx";

  public static final String SBOM_RESOURCE_PATH = API_VERSION_PATH_V2 + "sbom";

  public static final String SBOM_DASHBOARD_RESOURCE_PATH = SBOM_RESOURCE_PATH + "/dashboard";

  public static final String POLICY_VIOLATION_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "policyViolations";

  public static final String LEGACY_VIOLATIONS_PATH_V2 = API_VERSION_PATH_V2 + "legacyViolations";

  public static final String LEGACY_VIOLATIONS_CONFIG_PATH_V2 = API_VERSION_PATH_V2 + "config/legacyViolations";

  public static final String AUTO_POLICY_WAIVER_PATH = API_VERSION_PATH_V2 + "autoPolicyWaivers";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_PATH = API_VERSION_PATH_V2 +
      "autoPolicyWaiverExclusions";

  public static final String POLICY_WAIVER_PATH = API_VERSION_PATH_V2 + "policyWaivers";

  public static final String REPOSITORY_RESULTS_FOR_IMAGE_CONTAINER_PATH = API_VERSION_PATH_V2 + "firewall"
      + "/container-images/repositories/{ownerType: repository_container|repository_manager|repository}";

  public static final String POLICY_WAIVER_REASONS_PATH = API_VERSION_PATH_V2 + "policyWaiverReasons";

  public static final String POLICY_WAIVER_REQUEST_PATH = API_VERSION_PATH_V2 + "policyWaiverRequests";

  /**
   * Request header naming where a request originated. Values are {@code ScanSource} constant names;
   * unrecognized values are treated as the default rather than rejected.
   */
  public static final String X_SCAN_SOURCE_HEADER = "X-Scan-Source";

  public static final String POLICY_VIOLATION_WAIVER_PATH =
      API_VERSION_PATH_V2 + "policyWaiver/{policyViolationId}/{ownerType: application|organization}";

  public static final String SEARCH_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "search/component";

  public static final String COMPONENT_SEARCH_RESOURCE_PATH = API_VERSION_PATH_V2 + "componentSearch";

  public static final String REPORTS_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "reports";

  public static final String REPORT_DATA_RESOURCE_PATH_V2 = API_VERSION_PATH_V2
      + "applications/{applicationPublicId}/reports";

  public static final String HOSTED_REPOSITORY_COMPONENT_REPORT_DATA_RESOURCE_PATH_V2 =
      "/" + API_VERSION_PATH_V2 + "hostedRepositoryComponent/{hrcId}/reports";

  public static final String APPLICATION_EVALUATION_PATH_V2 = API_VERSION_PATH_V2 + "evaluation/applications";

  public static final String THIRD_PARTY_SCAN_PATH = API_VERSION_PATH_V2 + "scan/applications";

  public static final String POLICY_EVALUATION_STATUS_PATH_V2 =
      APPLICATION_EVALUATION_PATH_V2 + "/{applicationId}/status/{statusId}";

  public static final String COMPONENT_DETAILS_PATH_V2 = API_VERSION_PATH_V2 + "components/details";

  public static final String COMPONENT_QUARANTINE_RELEASE_PATH_V2 =
      API_VERSION_PATH_V2 + "repositories/quarantine/{quarantineId}/release";

  public static final String COMPONENT_VERSIONS_PATH_V2 = API_VERSION_PATH_V2 + "components/versions";

  public static final String APP_COMPONENT_LABELS_PATH_V2 = API_VERSION_PATH_V2
      + "components/{componentHash}/labels/{labelName}/{ownerType: application|organization}s/{internalOwnerId}";

  public static final String DATA_RETENTION_POLICY_RESOURCE_PATH = API_VERSION_PATH_V2 + "dataRetentionPolicies";

  public static final String VERSION_EVALUATION_WINDOW_RESOURCE_PATH = API_VERSION_PATH_V2 + "versionEvaluationWindow";

  public static final String COMPONENT_REMEDIATION_PATH_V2 =
      API_VERSION_PATH_V2 + "components/remediation/{ownerType: application|organization|repository}/{ownerId}";

  public static final String COMPONENT_REMEDIATION_BULK_PATH_V2 =
      API_VERSION_PATH_V2 + "components/remediation/{ownerType: application|organization|repository}/{ownerId}/bulk";

  public static final String SECURITY_VIOLATION_OVERRIDE_PATH_V2 = API_VERSION_PATH_V2 + "securityOverrides";

  public static final String PROXY_SERVER_CONFIG_PATH_V2 = API_VERSION_PATH_V2 + "config/httpProxyServer";

  public static final String REPOSITORY_CONNECTION_CONFIG_PATH_V2 = API_VERSION_PATH_V2 + "config/repositoryConnection";

  public static final String ARTIFACTORY_CONNECTION_CONFIG_PATH_V2 = API_VERSION_PATH_V2
      + "config/artifactoryConnection";

  public static final String SOURCE_CONTROL_PATH_V2 = API_VERSION_PATH_V2 + "sourceControl";

  public static final String SOURCE_CONTROL_METRICS_PATH_V2 = API_VERSION_PATH_V2 + "sourceControlMetrics";

  public static final String USER_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "users";

  public static final String VULNERABILITIES_RESOURCE_PATH_V2 =
      API_VERSION_PATH_V2 + "vulnerabilities/{refId}";

  public static final String BULK_VULNERABILITIES_RESOURCE_PATH_V2 =
      API_VERSION_PATH_V2 + "vulnerabilities";

  public static final String ROLE_MEMBERSHIP_PATH_V2 = API_VERSION_PATH_V2 + "roleMemberships";

  public static final String ROLE_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "roles";

  public static final String CONFIG_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "config";

  public static final String MAIL_CONFIG_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "config/mail";

  public static final String ZSCALER_CONFIG_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "config/zscaler";

  public static final String SAML_CONFIG_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "config/saml";

  public static final String OIDC_CONFIG_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "config/oidc";

  public static final String CROWD_CONFIG_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "config/crowd";

  public static final String REVERSE_PROXY_AUTHENTICATION_CONFIG_RESOURCE_PATH_V2 =
      API_VERSION_PATH_V2 + "config/reverseProxyAuthentication";

  public static final String JIRA_CONFIG_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "config/jira";

  public static final String SOURCE_CONTROL_CONFIG_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "config/sourceControl";

  public static final String GITHUB_APP_RESOURCE_PATH = API_VERSION_PATH_V2 + "githubApp";

  public static final String GITHUB_APP_SETUP_INSTALLATION_PATH = "setupInstallation";

  public static final String CI_CONFIG_RESOURCE_PATH_V2 =
      API_VERSION_PATH_V2 + "config/ci/{ownerType:application|organization}/{ownerId}";

  public static final String SCAN_HEALTH_CONFIG_PATH_V2 =
      API_VERSION_PATH_V2 + "config/scanHealth/{ownerType:application|organization}/{ownerId}";

  public static final String WAIVER_EXPIRATION_NOTIFICATION_CONFIG_PATH_V2 =
      API_VERSION_PATH_V2 +
          "waiverExpirationNotificationConfig/{ownerType:organization|repository_manager|repository_container}/{ownerId}";

  public static final String USER_TOKEN_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "userTokens";

  public static final String USER_TOKEN_CONFIG_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "config/userTokens";

  public static final String COMPOSITE_SOURCE_CONTROL_PATH_V2 = API_VERSION_PATH_V2 + "compositeSourceControl";

  public static final String CLAIM_PATH_V2 = API_VERSION_PATH_V2 + "claim/components";

  public static final String COMPOSITE_SOURCE_CONTROL_CONFIG_VALIDATOR_PATH_V2 =
      API_VERSION_PATH_V2 + "compositeSourceControlConfigValidator/application/{applicationId}";

  public static final String ADVANCED_SEARCH_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "search/advanced";

  public static final String LABEL_RESOURCE_PATH = API_VERSION_PATH_V2
      + "labels/{ownerType: application|organization|repository|repository_manager|repository_container|hosted_repository_component}/{ownerId}";

  public static final String APPLICATION_CATEGORY_RESOURCE_PATH = API_VERSION_PATH_V2 + "applicationCategories";

  public static final String CONFIG_FEATURES_PATH = API_VERSION_PATH_V2 + "config/features";

  public static final String LICENSE_LEGAL_RESOURCE_PATH = EXPERIMENTAL_PATH + "licenseLegalMetadata";

  public static final String LICENSE_LEGAL_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "licenseLegalMetadata";

  public static final String LICENSE_OVERRIDE_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 +
      "licenseOverrides/" +
      "{ownerType: application|organization|repository|repository_manager|repository_container"
      + "|hosted_repository_component}" +
      "/{ownerId}";

  public static final String PING_RESOURCE_PATH = "/ping";

  public static final String PRODUCT_LICENSE_RESOURCE_PATH = API_VERSION_PATH_V2 + "product/license";

  public static final String REPOSITORIES_RESOURCE_PATH = API_VERSION_PATH_V2 + "repositories";

  public static final String FIREWALL_RESOURCE_PATH = API_VERSION_PATH_V2 + "firewall";

  public static final String MALWARE_DEFENSE_RESOURCE_PATH = API_VERSION_PATH_V2 + "malware-defense";

  public static final String FIREWALL_CONTAINER_IMAGE_RESOURCE_PATH =
      FIREWALL_RESOURCE_PATH + "/container-image";

  public static final String FIREWALL_CASCADE_REEVALUATE_PATH =
      FIREWALL_RESOURCE_PATH + "/repositories/cascade-reevaluate";

  public static final String EXTERNAL_TELEMETRY_PATH = API_VERSION_PATH_V2 + "telemetry";

  public static final String REPOSITORY_IDENTIFIED_COMPONENT_PATH_V2 =
      API_VERSION_PATH_V2 + "repositoryIdentifiedComponent";

  public static final String SOURCE_CONTROL_EVENTS_RESOURCE_PATH =
      EXPERIMENTAL_PATH + "sourceControl/{ownerType:application|organization}/{ownerId}/events";

  public static final String ENDPOINTS_RESOURCE_PATH = API_VERSION_PATH_V2 + "endpoints";

  public static final String SOURCE_CONTROL_PATH_EXPERIMENTAL_PATH = EXPERIMENTAL_PATH + "sourceControl";

  public static final String EXPERIMENTAL_ONBOARDING_RESOURCE_PATH = EXPERIMENTAL_PATH + "onboarding";

  public static final String EXPERIMENTAL_VEX_ANALYSIS_DATA_PATH =
      EXPERIMENTAL_PATH + "vex/application/{applicationInternalId}/report/{scanId}";

  public static final String EXPERIMENTAL_SAST_SCAN_DATA_PATH =
      EXPERIMENTAL_PATH + "application/{applicationPublicId}/sastScan";

  public static final String EXPERIMENTAL_SAST_PATH =
      EXPERIMENTAL_PATH + "application/sast";

  public static final String CALL_FLOW_ANALYSIS_CONFIG =
      EXPERIMENTAL_PATH + "callFlowAnalysis/configuration/{ownerType: application|organization}/{ownerId}";

  public static final String AUDIT_LOGS_RESOURCE_PATH = API_VERSION_PATH_V2 + "auditLogs";

  public static final String USER_ACTIVITY_RESOURCE_PATH = API_VERSION_PATH_V2 + "userActivity";

  public static final String LICENSED_SOLUTIONS_RESOURCE_PATH = API_VERSION_PATH_V2 + "solutions/licensed";

  public static final String EXTERNAL_PATH = API_VERSION_PATH_V2 + "external";

  public static final String DISTRIBUTE_PATH = EXTERNAL_PATH + "/distribute";

  public static final String DEVELOPER_PATH = API_VERSION_PATH_V2 + "developer";

  public static final String COMPONENT_CHANGE_DETECTION_RESOURCE_PATH =
      API_VERSION_PATH_V2 + "component-change-detection";

  public static final String CPE_MATCHING_CONFIGURATION_RESOURCE_PATH = API_VERSION_PATH_V2 +
      "{ownerType: application|organization}/{internalOwnerId}/configuration/publicSource/cpe";

  public static final String REACHABILITY_EVIDENCE_RESOURCE_PATH =
      API_VERSION_PATH_V2 + "applications/{applicationPublicId}/reports/{reportId}/vulnerabilities";

  public static final String HOSTED_REPOSITORY_COMPONENT_REACHABILITY_EVIDENCE_RESOURCE_PATH =
      API_VERSION_PATH_V2 + "hostedRepositoryComponent/{hrcId}/reports/{reportId}/vulnerabilities";
}
