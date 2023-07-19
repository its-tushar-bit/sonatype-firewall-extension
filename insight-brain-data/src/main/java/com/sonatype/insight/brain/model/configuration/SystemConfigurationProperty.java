/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.33
 */
@Entity
@Table(name = "system_configuration_property")
public class SystemConfigurationProperty
    implements HasStringId
{
  public static final String AUTOMATIC_APPLICATION_CREATION_ENABLED = "AUTOMATIC_APPLICATION_CREATION_ENABLED";

  public static final String AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID =
      "AUTOMATIC_APPLICATION_CREATION_ORGANIZATION_ID";

  public static final String AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED =
      "AUTOMATIC_SOURCE_CONTROL_CONFIGURATION_ENABLED";

  public static final String ADVANCED_SEARCH_ENABLED = "ADVANCED_SEARCH_ENABLED";

  public static final String DASHBOARD_DISABLED = "DASHBOARD_DISABLED";

  public static final String QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS =
      "QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS";

  public static final String REPORTS_LIST_DISABLED = "REPORTS_LIST_DISABLED";

  public static final String FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED = "FIREWALL_INTEGRITY_RATING_LICENSE_ENABLED";

  public static final String SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED =
      "SECURITY_VULNERABILITY_SOURCE_CONDITION_DISABLED";

  public static final String BUILT_FROM_SOURCE = "BUILT_FROM_SOURCE";

  public static final String CROWD_INTEGRATION = "CROWD_INTEGRATION";

  public static final String WEBHOOK_CONFIGURATION = "WEBHOOK_CONFIGURATION";

  public static final String PRODUCT_LICENSE_CONFIGURATION = "PRODUCT_LICENSE_CONFIGURATION";

  public static final String LDAP_CONFIGURATION = "LDAP_CONFIGURATION";

  public static final String EMAIL_CONFIGURATION = "EMAIL_CONFIGURATION";

  public static final String PROXY_CONFIGURATION = "PROXY_CONFIGURATION";

  public static final String SYSTEM_NOTICE_CONFIGURATION = "SYSTEM_NOTICE_CONFIGURATION";

  public static final String SUCCESS_METRICS_CONFIGURATION = "SUCCESS_METRICS_CONFIGURATION";

  public static final String AUTOMATIC_APPLICATION_CONFIGURATION = "AUTOMATIC_APPLICATION_CONFIGURATION";

  public static final String AUTOMATIC_SCM_CONFIGURATION = "AUTOMATIC_SCM_CONFIGURATION";

  public static final String ADVANCED_SEARCH_CONFIGURATION = "ADVANCED_SEARCH_CONFIGURATION";

  public static final String TRANSITIVE_SOLVER_DISABLED = "TRANSITIVE_SOLVER_DISABLED";

  public static final String CODE_INSIGHTS = "CODE_INSIGHTS";

  public static final String COMPONENT_SEARCH_API_WITH_INNERSOURCE = "COMPONENT_SEARCH_API_WITH_INNERSOURCE";

  public static final String DEFAULT_BRANCH_MONITORING = "DEFAULT_BRANCH_MONITORING";

  public static final String DEPENDENCY_DATA_IN_API = "DEPENDENCY_DATA_IN_API";

  public static final String INNER_SOURCE_TRANSITIVE_WAIVER = "INNER_SOURCE_TRANSITIVE_WAIVER";

  public static final String INNER_SOURCE_REPOSITORY_INTEGRATION = "INNER_SOURCE_REPOSITORY_INTEGRATION";

  public static final String PR_COMMENTING = "PR_COMMENTING";

  public static final String PR_LINE_COMMENTING = "PR_LINE_COMMENTING";

  public static final String ENABLE_UNAUTHENTICATED_PAGES = "ENABLE_UNAUTHENTICATED_PAGES";

  public static final String ENABLE_SSO_ONLY = "ENABLE_SSO_ONLY";

  public static final String SSO_IDP_MANAGED_BY_SONATYPE = "SSO_IDP_MANAGED_BY_SONATYPE";

  public static final String INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS = "INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS";

  public static final String ACCESS_ALLOWLIST = "accessAllowlist";

  public static final String BASE_URL = "baseUrl";

  public static final String FORCE_BASE_URL = "forceBaseUrl";

  public static final String FRAME_ANCESTORS_ALLOWLIST = "frameAncestorsAllowlist";

  public static final String HDS_URL = "hdsUrl";

  public static final String CDN_URL = "cdnUrl";

  public static final String SUPPORT_READ_LIMIT_BYTES = "support.readLimitBytes";

  public static final String SUPPORT_CLUSTER_LOG_FILE_REGEX = "support.clusterLogFileRegex";

  public static final String EVENT_BUS_MAX_THREAD_POOL_SIZE = "eventBus.maxThreadPoolSize";

  public static final String CSRF_PROTECTION = "csrfProtection";

  public static final String USER_AGENT_SUFFIX = "userAgentSuffix";

  public static final String CSP_ENABLED = "cspEnabled";

  public static final String BLOCK_SEMICOLON_IN_PATH = "blockSemicolonInPath";

  public static final String BLOCK_BACKSLASH_IN_PATH = "blockBackslashInPath";

  public static final String BLOCK_NON_ASCII_IN_PATH = "blockNonAsciiInPath";

  public static final String RELEASE_GRAPH_CACHE_SIZE = "releaseGraphCacheSize";

  public static final String LICENSE_LEGAL_HDS_REQUEST_LIMIT = "licenseLegalHdsRequestLimit";

  public static final String MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD = "maxApplicationsToQueryOnDashboard";

  public static final String MAX_ADVANCED_SEARCH_CLAUSE_COUNT = "maxAdvancedSearchClauseCount";

  public static final String ADVANCED_SEARCH_CSV_EXPORT_DELIMITER = "advancedSearchCSVExportDelimiter";

  public static final String CONNECT_TIMEOUT_IN_SECONDS = "connectTimeoutInSeconds";

  public static final String SOCKET_TIMEOUT_IN_SECONDS = "socketTimeoutInSeconds";

  public static final String REPORT_TIMEOUT_IN_SECONDS = "reportTimeoutInSeconds";

  public static final String NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER =
      "needsAcknowledgementOfInitialDashboardFilter";

  public static final String ENABLE_DEFAULT_PASSWORD_WARNING = "enableDefaultPasswordWarning";

  public static final String POLICY_MONITORING_HOUR = "policyMonitoringHour";

  public static final String DB_BACKUP_DIR = "dbBackupDir";

  public static final String WEBHOOK_SECRET_PASSPHRASE = "webhookSecretPassphrase";

  public static final String EXTERNAL_HYPERLINKS_ALLOWED = "externalHyperlinksAllowed";

  public static final String MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING =
      "matcherConfiguration.disableConanNamespaceMatching";

  public static final String SCHEMA_MIGRATION_ENABLED = DatabaseMigrator.SCHEMA_MIGRATION_ENABLED;

  public static final String API_PAGE = "API_PAGE";

  public static final String INTEGRATIONS_PAGE = "INTEGRATIONS_PAGE";

  public static final String SESSION_TIMEOUT_MINUTES = "sessionTimeout";

  public static final String BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX = "bfs.artifactoryExpiredTokenRegex";

  public static final String BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL = "bfs.artifactoryExpiredTokenEmail";

  public static final String BFS_COMPONENT_QUERY_LIMIT = "bfs.componentQueryLimit";

  public static final String BFS_REPOSITORIES = "bfs.repositories";

  public static final String SCAN_POM_FILES_IN_META_INF_DIRECTORY = "scanPomFilesInMetaInfDirectory";

  public static final String PURGE_SCAN_FILES = "purgeScanFiles";

  public static final String SCAN_NPM_DEV_AND_OPT_DEPENDENCIES = "scanNpmDevAndOptDependencies";

  public static final String AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES =
      "automaticQuarantineReleaseTimeIntervalInMinutes";

  public static final String WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR = "waivedComponentUpgradeInspectionHour";

  public static final String LOGOUT_AUTH0_ON_LOGOUT = "logoutAuth0OnLogout";

  public static final String WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED = "waivedComponentUpgradeMonitoringEnabled";

  public static final String ALP_OBSERVED_LICENSE_DETECTION_ENABLED = "alpObservedLicenseDetectionEnabled";

  public static final String QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS =
      "quarantinedComponentReportExpirationTimeInHours";

  public static final String QUARANTINED_ITEM_CUSTOM_MESSAGE = "quarantinedItemCustomMessage";

  public static final String SCM_UX_IMPROVEMENTS = "scmUxImprovements";

  /**
   * Internal feature flag to enable Firewall Onboarding. It will be removed right before we release Firewall
   * Onboarding for external customers.
   */
  public static final String INTERNAL_FIREWALL_ONBOARDING_ENABLED = "internalFirewallOnboardingEnabled";

  @Id
  @Column(name = "system_configuration_property_id")
  private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "value")
  private String value;

  public SystemConfigurationProperty() {
  }

  public SystemConfigurationProperty(String name, String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
