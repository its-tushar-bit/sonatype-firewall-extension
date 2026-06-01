/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonatype.insight.brain.spring.config.DropwizardLoggingConfig;
import com.sonatype.insight.brain.spring.config.DropwizardServerConfig;
import com.sonatype.insight.brain.spring.config.DropwizardWebConfig;
import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.eventbus.EventBusConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.brain.migration.JiraConfigurationMigrator;
import com.sonatype.insight.brain.migration.MailConfigurationMigrator;
import com.sonatype.insight.brain.migration.ProxyServerConfigurationMigrator;
import com.sonatype.insight.brain.migration.ReverseProxyAuthenticationConfigurationMigrator.ReverseProxyAuthenticationConfig;
import com.sonatype.insight.brain.migration.SourceControlConfigurationMigrator;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.security.AllowedIp;
import com.sonatype.insight.brain.service.config.StorageConfig;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main configuration class for Nexus IQ Server.
 * Migrated from Dropwizard Configuration to Spring Boot @ConfigurationProperties.
 */
public class InsightConfig
{
  private static final Logger log = LoggerFactory.getLogger(InsightConfig.class);

  public static final String DEFAULT_BACKUP_DIR = "db-backup";

  public static final String NXIQ_SUPPORT_CLUSTER_LOG_FILE_REGEX = "NXIQ_SUPPORT_CLUSTER_LOG_FILE_REGEX";

  public static final String DEFAULT_SUPPORT_CLUSTER_LOG_FILE_REGEX = "^.*log[\\\\/].*log$";

  public InsightConfig() {
    // Default constructor for YAML deserialization via DropwizardConfigLoader
  }

  @JsonProperty
  private ProxyServerConfigurationMigrator.ProxyConfig proxy;

  @JsonProperty
  private MailConfigurationMigrator.MailConfig mail;

  @JsonProperty
  private String baseUrl;

  /**
   * When true, {@link BaseUrl#get()} will always return the configured {@link #baseUrl}.
   *
   * @since 1.41
   */
  @JsonProperty
  private Boolean forceBaseUrl;

  @JsonProperty
  private String hdsUrl;

  @JsonProperty
  private String cdnUrl;

  @NotNull
  @JsonProperty
  protected String sonatypeWork = "sonatype-work/clm-server";

  /**
   * @since 1.99
   */
  @JsonProperty
  protected String clusterDirectory;

  @JsonProperty
  private SupportConfig support;

  /**
   * @since 1.65
   */
  @Valid
  @JsonProperty
  private DatabaseConfig database;

  /**
   * The directory where db backups are created. If set to a relative path, then it is considered relative to the
   * {@link sonatypeWork} directory.
   *
   * @since 1.15.0
   */
  @JsonProperty
  private String dbBackupDir;

  @JsonProperty
  private Integer releaseGraphCacheSize;

  /**
   * will be appended to the jdbc url, primarily intended for diagnostic usage
   */
  @JsonProperty
  private String additionalDBParams;

  /**
   * @since 1.14.2
   */
  @Min(1)
  @Max(99)
  @JsonProperty
  private Integer dbCacheSizePercent;

  @JsonProperty
  private Integer policyMonitoringHour;

  /**
   * @since 1.16.0
   */
  @JsonProperty
  private Boolean csrfProtection;

  /**
   * @since 1.14.0
   */
  @JsonProperty
  private String userAgentSuffix;

  /**
   * @since 1.16.0
   */
  @JsonProperty
  private ReverseProxyAuthenticationConfig reverseProxyAuthentication;

  /**
   * @since 1.21.0
   */
  @JsonProperty
  private JiraConfigurationMigrator.JiraConfig jira;

  /**
   * @since 1.20
   */
  @NotNull
  @JsonProperty
  private boolean exitOnFatalError = true;

  /**
   * @since 1.25.0
   */
  @JsonProperty
  private String webhookSecretPassphrase;

  /**
   * @since 1.193.0
   */
  @JsonProperty
  private String webhookSecretPassphraseFips;

  /**
   * @since 1.25.0
   */
  @JsonProperty
  private EventBusConfig eventBus;

  /**
   * @since 1.32
   */
  @JsonProperty
  private Integer reportTimeoutInSeconds;

  /**
   * If true, users must configure and acknowledge a filter before being able to see any data in the dashboard.
   *
   * @since 1.29
   */
  @JsonProperty
  private Boolean needsAcknowledgementOfInitialDashboardFilter;

  /**
   * @since 1.43
   */
  @NotNull
  @JsonProperty
  private boolean createSampleData = false;

  /**
   * @since 1.44
   */
  @NotNull
  @JsonProperty
  private boolean importReferencePoliciesFromHDS = true;

  /**
   * @since 1.47
   */
  @JsonProperty
  private Boolean enableDefaultPasswordWarning;

  /**
   * @since 1.52
   */
  @JsonProperty
  private String licenseFile;

  /**
   * @since 1.55
   */
  @JsonProperty
  private Boolean externalHyperlinksAllowed;

  /**
   * @since 1.59
   *        This configuration disables the HTTP CSP header. It only exists because that header breaks the Geb
   *        functional
   *        tests
   */
  @JsonProperty
  private Boolean cspEnabled;

  /**
   * @since 1.73
   */
  @JsonProperty
  private SourceControlConfigurationMigrator.SourceControlConfig sourceControl;

  /**
   * This section will be used for features that are enabled by default. If nothing is specified, or the feature flag is
   * set to {@code true}, the feature is enabled. To disable it, explicitly set it to {@code false}.
   * <p>
   * For example:
   *
   * <pre>
   * # features are enabled by default - set to false if you want to disable them
   * features:
   *   prCommenting: false
   * }
   * </pre>
   *
   * @since 1.90
   */
  @JsonProperty
  private Map<String, Boolean> features;

  /**
   * This configuration blocks requests containing semicolons in the path to avoid malicious attacks.
   *
   * @since 1.98
   */
  @JsonProperty
  private Boolean blockSemicolonInPath;

  /**
   * This configuration blocks requests containing backslash in the path to avoid malicious attacks.
   *
   * @since 1.98
   */
  @JsonProperty
  private Boolean blockBackslashInPath;

  /**
   * This configuration blocks requests containing non-ASCII characters in the path to avoid malicious attacks.
   * Since shiro:1.7.0, the InvalidRequestFilter.isAccessAllowed() method checks for non-ascii chars
   * both the encoded URI and the decoded path.
   * Since we allow non-ascii chars in our URLs (for ex, in app public IDs that are used in REST paths),
   * we cannot enable this check.
   *
   * @since 1.98
   */
  @JsonProperty
  private Boolean blockNonAsciiInPath;

  /**
   * This configuration limits the number of parallel requests for license data made to HDS for the Advanced Legal Pack.
   *
   * @since 1.101
   */
  @JsonProperty
  private Integer licenseLegalHdsRequestLimit;

  /**
   * This configuration allows adjusting/tuning matcher behaviours based on specific customer needs.
   *
   * @since 1.101
   */
  @JsonProperty
  private Map<String, String> matcherConfiguration;

  /**
   * This configuration allows adjusting the time the default branch monitor is executed
   *
   * @since 1.117
   */
  @JsonProperty
  private SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig defaultBranchMonitoring;

  /**
   * This configuration limits the number of applications that can be queried by the dashboard services
   *
   * @since 1.126
   */
  @JsonProperty
  private Integer maxApplicationsToQueryOnDashboard;

  /**
   * HSTS (HTTP Strict Transport Security) configuration, migrated from Dropwizard's
   * {@code web.hsts} section. HSTS is enabled by default to match legacy behavior.
   */
  @JsonIgnore
  private HstsConfig hstsConfig = new HstsConfig();

  /**
   * X-Frame-Options configuration, compatible with legacy Dropwizard {@code web.frame-options} settings.
   * Enabled by default with a value of {@code DENY} to preserve the current Spring migration behavior.
   */
  @JsonIgnore
  private FrameOptionsConfig frameOptionsConfig = new FrameOptionsConfig();

  /**
   * This configuration limits the number of clauses a query can contain when using Advanced Search
   *
   * @since 1.136
   */
  @JsonProperty
  private Integer maxAdvancedSearchClauseCount;

  /**
   * Default delimiter for Advanced Search CSV export file
   *
   * @since 1.137
   */
  @JsonProperty
  private String advancedSearchCSVExportDelimiter;

  @Valid
  @JsonProperty(value = "storage")
  private StorageConfig storage = new StorageConfig();

  @Valid
  @JsonProperty(value = "search")
  private SearchConfig searchConfig;

  @JsonProperty
  private DropwizardServerConfig server;

  @JsonProperty
  private DropwizardLoggingConfig logging;

  @JsonProperty
  private DropwizardWebConfig web;

  @Deprecated
  @JsonProperty
  private Object metrics;

  @JsonProperty
  private Object admin;

  @Deprecated
  @JsonProperty
  private Object health;

  public DropwizardServerConfig getServer() {
    return server;
  }

  public DropwizardLoggingConfig getLogging() {
    return logging;
  }

  public DropwizardWebConfig getWeb() {
    return web;
  }

  public ProxyServerConfigurationMigrator.ProxyConfig getProxyConfig() {
    return proxy;
  }

  public MailConfigurationMigrator.MailConfig getMailConfig() {
    return mail;
  }

  public Integer getReleaseGraphCacheSize() {
    return releaseGraphCacheSize;
  }

  public String getHdsUrl() {
    return hdsUrl;
  }

  public File getSonatypeWork() {
    return new File(sonatypeWork);
  }

  @JsonIgnore
  public File getClusterDirectory() {
    if (clusterDirectory == null) {
      return getSonatypeWork();
    }
    return new File(clusterDirectory);
  }

  @JsonIgnore
  public boolean isClusterDirectorySetByUser() {
    return clusterDirectory != null;
  }

  @JsonIgnore
  @AssertTrue(message = "Cannot set sonatypeWork as the clusterDirectory.")
  public boolean isValidClusterDirectory() {
    try {
      if (clusterDirectory == null) {
        return true;
      }
      Files.createDirectories(getSonatypeWork().toPath());
      Files.createDirectories(getClusterDirectory().toPath());
      return !Files.isSameFile(getSonatypeWork().toPath(), getClusterDirectory().toPath());
    }
    catch (Exception e) {
      log.error("Invalid clusterDirectory: {}", e.getMessage());
      return false;
    }
  }

  @JsonIgnore
  public File getConfigDir() {
    return new File(sonatypeWork, "config");
  }

  public void setProxyConfig(ProxyServerConfigurationMigrator.ProxyConfig proxyConfig) {
    this.proxy = proxyConfig;
  }

  public void setMailConfig(final MailConfigurationMigrator.MailConfig mailConfig) {
    this.mail = mailConfig;
  }

  public void setReleaseGraphCacheSize(Integer releaseGraphCacheSize) {
    this.releaseGraphCacheSize = releaseGraphCacheSize;
  }

  public void setHdsUrl(final String hdsUrl) {
    this.hdsUrl = hdsUrl;
  }

  @SuppressWarnings("unused")
  // for Jackson, supports deserialization of configs from 1.15-
  private void setSaasAddress(final String hdsUrl) {
    setHdsUrl(hdsUrl);
  }

  public void setSonatypeWork(final String sonatypeWork) {
    this.sonatypeWork = sonatypeWork;
  }

  public void setClusterDirectory(String clusterDirectory) {
    this.clusterDirectory = clusterDirectory;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * @since 1.41
   */
  public Boolean isForceBaseUrl() {
    return forceBaseUrl;
  }

  /**
   * @since 1.41
   */
  public void setForceBaseUrl(Boolean forceBaseUrl) {
    this.forceBaseUrl = forceBaseUrl;
  }

  public String getCdnUrl() {
    return cdnUrl;
  }

  public void setCdnUrl(String cdnUrl) {
    this.cdnUrl = cdnUrl;
  }

  // isValidApplicationConnectors removed - Spring Boot manages server connectors via server.port properties

  /**
   * @since 1.8
   */
  public Integer getPolicyMonitoringHour() {
    return policyMonitoringHour;
  }

  /**
   * @since 1.8
   */
  public void setPolicyMonitoringHour(final Integer policyMonitoringHour) {
    this.policyMonitoringHour = policyMonitoringHour;
  }

  public String getAdditionalDBParams() {
    return additionalDBParams;
  }

  public void setAdditionalDBParams(final String additionalDBParams) {
    this.additionalDBParams = additionalDBParams;
  }

  public Integer getDbCacheSizePercent() {
    return dbCacheSizePercent;
  }

  public void setDbCacheSizePercent(Integer dbCacheSizePercent) {
    this.dbCacheSizePercent = dbCacheSizePercent;
  }

  /**
   * @since 1.14.0
   * @deprecated The support for anonymous client access was removed in 1.72.
   */
  @Deprecated
  public void setAnonymousClientAccessAllowed(@SuppressWarnings("unused") final boolean anonymousClientAccessAllowed) {
    log.warn("The support for anonymous client access was removed in Nexus IQ Server 72. "
        + "The anonymousClientAccessAllowed configuration option should be removed from the config yml file.");
  }

  public Boolean isCsrfProtection() {
    return csrfProtection;
  }

  public void setCsrfProtection(Boolean csrfProtection) {
    this.csrfProtection = csrfProtection;
  }

  /**
   * @since 1.14.0
   */
  public String getUserAgentSuffix() {
    return userAgentSuffix;
  }

  /**
   * @since 1.14.0
   */
  public void setUserAgentSuffix(final String userAgentSuffix) {
    this.userAgentSuffix = userAgentSuffix;
  }

  /**
   * @since 1.15.0
   */
  public String getDbBackupDir() {
    return dbBackupDir;
  }

  /**
   * @deprecated Removed in 1.98.
   */
  @Deprecated
  public void setShowRootOrganization(@SuppressWarnings("unused") boolean showRootOrganization) {
    log.warn("The support for hiding the root organization was removed in Nexus IQ Server 98. "
        + "The showRootOrganization configuration option should be removed from the config yml file.");
  }

  public void setDbBackupDir(String dbBackupDir) {
    this.dbBackupDir = dbBackupDir;
  }

  public ReverseProxyAuthenticationConfig getReverseProxyAuthentication() {
    return reverseProxyAuthentication;
  }

  public void setReverseProxyAuthentication(ReverseProxyAuthenticationConfig reverseProxyAuthentication) {
    this.reverseProxyAuthentication = reverseProxyAuthentication;
  }

  /**
   * @since 1.21.0
   */
  public JiraConfigurationMigrator.JiraConfig getJiraConfig() {
    return jira;
  }

  /**
   * @since 1.21.0
   */
  public void setJiraConfig(JiraConfigurationMigrator.JiraConfig jira) {
    this.jira = jira;
  }

  public boolean isExitOnFatalError() {
    return exitOnFatalError;
  }

  public void setExitOnFatalError(boolean exitOnFatalError) {
    this.exitOnFatalError = exitOnFatalError;
  }

  /**
   * @since 1.21
   * @deprecated Removed in 1.97.
   */
  @Deprecated
  public void setRepositoryPolicyViolationNotificationInterval(
      @SuppressWarnings("unused") int repositoryPolicyViolationNotificationInterval)
  {
    log.warn("The support for repository policy violation notification interval was removed in Nexus IQ Server 97. "
        + "The repositoryPolicyViolationNotificationInterval configuration option should be removed from the "
        + "config yml file.");
  }

  public EventBusConfig getEventBusConfig() {
    return eventBus;
  }

  public void setEventBusConfig(final EventBusConfig eventBusConfig) {
    this.eventBus = eventBusConfig;
  }

  public String getWebhookSecretPassphrase() {
    return webhookSecretPassphrase;
  }

  public void setWebhookSecretPassphrase(final String webhookSecretPassphrase) {
    this.webhookSecretPassphrase = webhookSecretPassphrase;
  }

  public String getWebhookSecretPassphraseFips() {
    return webhookSecretPassphraseFips;
  }

  public void setWebhookSecretPassphraseFips(final String webhookSecretPassphraseFips) {
    this.webhookSecretPassphraseFips = webhookSecretPassphraseFips;
  }

  /**
   * @since 1.27
   */
  public void setSupportConfig(final SupportConfig supportConfig) {
    this.support = supportConfig;
  }

  /**
   * @since 1.27
   */
  public SupportConfig getSupportConfig() {
    return support;
  }

  /**
   * If true, users must configure and acknowledge a filter before being able to see any data in the dashboard.
   *
   * @since 1.29
   */
  public Boolean isNeedsAcknowledgementOfInitialDashboardFilter() {
    return needsAcknowledgementOfInitialDashboardFilter;
  }

  /**
   * @since 1.29
   */
  public void setNeedsAcknowledgementOfInitialDashboardFilter(Boolean needsAcknowledgementOfInitialDashboardFilter) {
    this.needsAcknowledgementOfInitialDashboardFilter = needsAcknowledgementOfInitialDashboardFilter;
  }

  /**
   * If true, sample data is created for new installs.
   *
   * @since 1.43
   */
  public boolean isCreateSampleData() {
    return createSampleData;
  }

  /**
   * @since 1.43
   */
  public void setCreateSampleData(boolean createSampleData) {
    this.createSampleData = createSampleData;
  }

  /**
   * If true, references policies are downloaded from HDS for new installs.
   *
   * @since 1.44
   */
  public boolean isImportReferencePoliciesFromHDS() {
    return importReferencePoliciesFromHDS;
  }

  /**
   * @since 1.44
   */
  public void setImportReferencePoliciesFromHDS(boolean importReferencePoliciesFromHDS) {
    this.importReferencePoliciesFromHDS = importReferencePoliciesFromHDS;
  }

  /**
   * The time in seconds that IQ server is expected to wait for the report before timing out.
   *
   * @since 1.32
   */
  public Integer getReportTimeoutInSeconds() {
    return reportTimeoutInSeconds;
  }

  /**
   * @since 1.32
   */
  public void setReportTimeoutInSeconds(final Integer reportTimeoutInSeconds) {
    this.reportTimeoutInSeconds = reportTimeoutInSeconds;
  }

  /**
   * @deprecated removed in Jan 2024 as we no longer have any customers running an IQ this old. See CLM-29089.
   */
  @Deprecated
  public void setConsentToUpgradeToVersion_1_45(@SuppressWarnings("unused") boolean consentToUpgradeToVersion_1_45) {
    log.warn("The consentToUpgradeToVersion_1_45 configuration option is obsolete and can be removed from the " +
        "config yml file.");
  }

  public Boolean isEnableDefaultPasswordWarning() {
    return enableDefaultPasswordWarning;
  }

  public void setEnableDefaultPasswordWarning(Boolean enableDefaultPasswordWarning) {
    this.enableDefaultPasswordWarning = enableDefaultPasswordWarning;
  }

  public String getLicenseFile() {
    return licenseFile;
  }

  public void setLicenseFile(final String licenseFile) {
    this.licenseFile = licenseFile;
  }

  public Boolean isExternalHyperlinksAllowed() {
    return externalHyperlinksAllowed;
  }

  public void setExternalHyperlinksAllowed(final Boolean externalHyperlinksAllowed) {
    this.externalHyperlinksAllowed = externalHyperlinksAllowed;
  }

  public Boolean isCspEnabled() {
    return cspEnabled;
  }

  public void setCspEnabled(final Boolean cspEnabled) {
    this.cspEnabled = cspEnabled;
  }

  public DatabaseConfig getDatabase() {
    return database;
  }

  public void setDatabase(DatabaseConfig database) {
    this.database = database;
  }

  @JsonIgnore
  public boolean isDatabaseEmbedded() {
    return database == null;
  }

  /**
   * @since 1.67
   */
  @JsonProperty
  private Integer connectTimeoutInSeconds;

  /**
   * This uses a generous default value to account for batched component data requests that are known to occasionally
   * take ~1 minute.
   *
   * @since 1.101
   */
  @JsonProperty
  private Integer socketTimeoutInSeconds;

  /**
   * @since 1.114
   */
  @JsonProperty
  private Integer pullRequestMonitoringIntervalInSeconds;

  /**
   * @since 1.152
   */
  @JsonProperty
  private List<AllowedIp> systemAllowlist;

  public List<AllowedIp> getSystemAllowlist() {
    return systemAllowlist == null ? new ArrayList<>() : systemAllowlist;
  }

  public void setSystemAllowlist(List<AllowedIp> systemAllowlist) {
    this.systemAllowlist = systemAllowlist;
  }

  public Integer getConnectTimeoutInSeconds() {
    return connectTimeoutInSeconds;
  }

  public void setConnectTimeoutInSeconds(Integer connectTimeoutInSeconds) {
    this.connectTimeoutInSeconds = connectTimeoutInSeconds;
  }

  public Integer getSocketTimeoutInSeconds() {
    return socketTimeoutInSeconds;
  }

  public void setSocketTimeoutInSeconds(Integer socketTimeoutInSeconds) {
    this.socketTimeoutInSeconds = socketTimeoutInSeconds;
  }

  public SourceControlConfigurationMigrator.SourceControlConfig getSourceControl() {
    return sourceControl;
  }

  public void setSourceControl(SourceControlConfigurationMigrator.SourceControlConfig sourceControl) {
    this.sourceControl = sourceControl;
  }

  /**
   * Returns a map of declared feature flags and their states i.e. enabled or disabled.
   *
   * @see InsightConfig#features for details on how feature flags are specified
   */
  public Map<String, Boolean> getFeatures() {
    return features;
  }

  /**
   * Returns {@code true} if a feature flag, identified by name, is enabled; returns {@code false} otherwise.
   * <p>
   * If the feature flag is not explicitly declared it is considered enabled by default.
   *
   * @see InsightConfig#features for details on how feature flags are specified
   */
  @VisibleForTesting
  boolean isFeatureEnabled(String feature) {
    return features == null || !features.containsKey(feature) || features.containsKey(feature) && features.get(feature);
  }

  /**
   * Returns {@code true} if a feature flag is enabled; returns {@code false} otherwise.
   * <p>
   * If the feature flag is not explicitly declared it is considered enabled by default.
   *
   * @see InsightConfig#features for details on how feature flags are specified
   */
  public boolean isFeatureEnabled(Feature feature) {
    return isFeatureEnabled(feature.flag);
  }

  public void setFeatures(final Map<String, Boolean> features) {
    this.features = features;
  }

  public Boolean isBlockSemicolonInPath() {
    return blockSemicolonInPath;
  }

  public void setBlockSemicolonInPath(Boolean blockSemicolonInPath) {
    this.blockSemicolonInPath = blockSemicolonInPath;
  }

  public Boolean isBlockBackslashInPath() {
    return blockBackslashInPath;
  }

  public void setBlockBackslashInPath(Boolean blockBackslashInPath) {
    this.blockBackslashInPath = blockBackslashInPath;
  }

  public Boolean isBlockNonAsciiInPath() {
    return blockNonAsciiInPath;
  }

  public void setBlockNonAsciiInPath(Boolean blockNonAsciiInPath) {
    this.blockNonAsciiInPath = blockNonAsciiInPath;
  }

  public Integer getLicenseLegalHdsRequestLimit() {
    return licenseLegalHdsRequestLimit;
  }

  public void setLicenseLegalHdsRequestLimit(Integer licenseLegalHdsRequestLimit) {
    this.licenseLegalHdsRequestLimit = licenseLegalHdsRequestLimit;
  }

  public SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig getDefaultBranchMonitoring() {
    return defaultBranchMonitoring;
  }

  public void setDefaultBranchMonitoring(
      SourceControlConfigurationMigrator.DefaultBranchMonitoringConfig defaultBranchMonitoring)
  {
    this.defaultBranchMonitoring = defaultBranchMonitoring;
  }

  public Map<String, String> getMatcherConfiguration() {
    return matcherConfiguration;
  }

  public void setMatcherConfiguration(Map<String, String> matcherConfiguration) {
    this.matcherConfiguration = matcherConfiguration;
  }

  /**
   * This enumeration contains features that are enabled by default (not experimental). If nothing is specified, or
   * the feature flag is set to {@code true}, the feature is enabled. To disable it, explicitly set it to {@code false}.
   * <p>
   * For example:
   *
   * <pre>
   * # features are enabled by default - set to false if you want to disable them
   * features:
   *   prCommenting: false
   * }
   * </pre>
   */
  public enum Feature
      implements
      com.sonatype.insight.license.model.Feature
  {
    CODE_INSIGHTS("codeInsights"),
    COMPONENT_SEARCH_API_WITH_INNERSOURCE("componentSearchApiWithInnerSource"),
    DEFAULT_BRANCH_MONITORING("defaultBranchMonitoring"),
    DEPENDENCY_DATA_IN_API("dependencyDataInApi"),
    INNER_SOURCE_TRANSITIVE_WAIVER("innerSourceTransitiveWaiver"),
    INNER_SOURCE_REPOSITORY_INTEGRATION("innerSourceRepositoryIntegration"),
    PR_COMMENTING("prCommenting"),
    PR_LINE_COMMENTING("prLineCommenting"),
    ENABLE_UNAUTHENTICATED_PAGES("enableUnauthenticatedPages"),
    ENABLE_SSO_ONLY("enableSsoOnly"),

    /**
     * @deprecated Use {@link SourceControl#getSourceControlEvaluationsEnabled() instead}
     */
    @Deprecated
    INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS("internalSourceControlPolicyEvaluations");

    private final String flag;

    Feature(final String flag) {
      this.flag = flag;
    }

    public String getFlag() {
      return flag;
    }
  }

  public Integer getPullRequestMonitoringIntervalInSeconds() {
    return pullRequestMonitoringIntervalInSeconds;
  }

  public void setPullRequestMonitoringIntervalInSeconds(Integer pullRequestMonitoringIntervalInSeconds) {
    this.pullRequestMonitoringIntervalInSeconds = pullRequestMonitoringIntervalInSeconds;
  }

  public Integer getMaxApplicationsToQueryOnDashboard() {
    return maxApplicationsToQueryOnDashboard;
  }

  public void setMaxApplicationsToQueryOnDashboard(Integer maxApplicationsToQueryOnDashboard) {
    this.maxApplicationsToQueryOnDashboard = maxApplicationsToQueryOnDashboard;
  }

  /**
   * @deprecated The initial admin password must now be set using an environment variable
   *             {@link com.sonatype.insight.brain.migration.AdminInitialPasswordMigrator#NXIQ_INITIAL_ADMIN_PASSWORD}
   */
  @Deprecated
  public void setInitialAdminPassword(@SuppressWarnings("unused") String initialAdminPassword) {
    log.warn("The support for initial admin password setting via config.yml was removed in Nexus IQ Server 142. "
        + "Use the NXIQ_INITIAL_ADMIN_PASSWORD environment variable instead.");
  }

  public HstsConfig getHstsConfig() {
    return hstsConfig;
  }

  public void setHstsConfig(HstsConfig hstsConfig) {
    this.hstsConfig = hstsConfig;
  }

  public FrameOptionsConfig getFrameOptionsConfig() {
    return frameOptionsConfig;
  }

  public void setFrameOptionsConfig(FrameOptionsConfig frameOptionsConfig) {
    this.frameOptionsConfig = frameOptionsConfig;
  }

  public Integer getMaxAdvancedSearchClauseCount() {
    return maxAdvancedSearchClauseCount;
  }

  public void setMaxAdvancedSearchClauseCount(Integer maxAdvancedSearchClauseCount) {
    this.maxAdvancedSearchClauseCount = maxAdvancedSearchClauseCount;
  }

  public String getAdvancedSearchCSVExportDelimiter() {
    return advancedSearchCSVExportDelimiter;
  }

  public void setAdvancedSearchCSVExportDelimiter(String advancedSearchCSVExportDelimiter) {
    this.advancedSearchCSVExportDelimiter = advancedSearchCSVExportDelimiter;
  }

  public void setStorage(StorageConfig storage) {
    this.storage = storage;
  }

  public StorageConfig getStorage() {
    return storage;
  }

  @JsonIgnore
  @AssertTrue(message = "Invalid storage configuration")
  public boolean isValidStorageConfig() {
    StorageConfig storage = getStorage();
    try {
      if (storage != null) {
        storage.validate();
      }
      return true;
    }
    catch (ValidationException e) {
      log.error("Invalid storage configuration: {}", e.getMessage());
      return false;
    }
  }

  public SearchConfig getSearchConfig() {
    return searchConfig;
  }

  public void setSearchConfig(final SearchConfig searchConfig) {
    this.searchConfig = searchConfig;
  }

  // getApplicationConnectorPorts removed - Spring Boot manages ports via server.port property
  // Added back for backwards compatibility with telemetry ID generation
  @JsonIgnore
  private String applicationConnectorPorts = "8070";

  @JsonIgnore
  private String applicationConnectorTypes = "http";

  @JsonIgnore
  private String adminConnectorTypes = "http";

  @JsonIgnore
  private String serverLogFilename;

  @JsonIgnore
  private String requestLogFilename;

  @JsonIgnore
  private String auditLogFilename;

  @JsonIgnore
  private String policyViolationLogFilename;

  @JsonIgnore
  public String getApplicationConnectorPorts() {
    return applicationConnectorPorts;
  }

  /**
   * Sets the application connector ports string for telemetry ID generation.
   * Used primarily for testing.
   */
  @JsonIgnore
  public void setApplicationConnectorPorts(String ports) {
    this.applicationConnectorPorts = ports;
  }

  @JsonIgnore
  public String getApplicationConnectorTypes() {
    return applicationConnectorTypes;
  }

  @JsonIgnore
  public void setApplicationConnectorTypes(String applicationConnectorTypes) {
    this.applicationConnectorTypes = applicationConnectorTypes;
  }

  @JsonIgnore
  public String getAdminConnectorTypes() {
    return adminConnectorTypes;
  }

  @JsonIgnore
  public void setAdminConnectorTypes(String adminConnectorTypes) {
    this.adminConnectorTypes = adminConnectorTypes;
  }

  @JsonIgnore
  public String getServerLogFilename() {
    return serverLogFilename;
  }

  @JsonIgnore
  public void setServerLogFilename(String serverLogFilename) {
    this.serverLogFilename = serverLogFilename;
  }

  @JsonIgnore
  public String getRequestLogFilename() {
    return requestLogFilename;
  }

  @JsonIgnore
  public void setRequestLogFilename(String requestLogFilename) {
    this.requestLogFilename = requestLogFilename;
  }

  @JsonIgnore
  public String getAuditLogFilename() {
    return auditLogFilename;
  }

  @JsonIgnore
  public void setAuditLogFilename(String auditLogFilename) {
    this.auditLogFilename = auditLogFilename;
  }

  @JsonIgnore
  public String getPolicyViolationLogFilename() {
    return policyViolationLogFilename;
  }

  @JsonIgnore
  public void setPolicyViolationLogFilename(String policyViolationLogFilename) {
    this.policyViolationLogFilename = policyViolationLogFilename;
  }

  /**
   * HSTS configuration, compatible with legacy Dropwizard {@code web.hsts} settings.
   * Enabled by default with a max-age of 365 days and includeSubDomains=true,
   * matching the prior Dropwizard WebConfiguration defaults.
   */
  public static class HstsConfig
  {
    private boolean enabled = true;

    private long maxAgeSeconds = 365L * 24 * 60 * 60; // 365 days in seconds

    private boolean includeSubDomains = true;

    private boolean preload = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public long getMaxAgeSeconds() {
      return maxAgeSeconds;
    }

    public void setMaxAgeSeconds(long maxAgeSeconds) {
      this.maxAgeSeconds = maxAgeSeconds;
    }

    public boolean isIncludeSubDomains() {
      return includeSubDomains;
    }

    public void setIncludeSubDomains(boolean includeSubDomains) {
      this.includeSubDomains = includeSubDomains;
    }

    public boolean isPreload() {
      return preload;
    }

    public void setPreload(boolean preload) {
      this.preload = preload;
    }

    /**
     * Build the Strict-Transport-Security header value.
     */
    public String buildHeaderValue() {
      StringBuilder sb = new StringBuilder();
      sb.append("max-age=").append(maxAgeSeconds);
      if (includeSubDomains) {
        sb.append("; includeSubDomains");
      }
      if (preload) {
        sb.append("; preload");
      }
      return sb.toString();
    }
  }

  /**
   * X-Frame-Options configuration, compatible with legacy Dropwizard {@code web.frame-options} settings.
   */
  public static class FrameOptionsConfig
  {
    public enum FrameOption
    {
      DENY("DENY"),
      SAMEORIGIN("SAMEORIGIN"),
      ALLOW_FROM("ALLOW-FROM");

      private final String value;

      FrameOption(final String value) {
        this.value = value;
      }

      public String getValue() {
        return value;
      }
    }

    private boolean enabled = false;

    private FrameOption option = FrameOption.DENY;

    private String origin;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }

    public FrameOption getOption() {
      return option;
    }

    public void setOption(final FrameOption option) {
      this.option = option;
    }

    public String getOrigin() {
      return origin;
    }

    public void setOrigin(final String origin) {
      this.origin = origin;
    }

    public String buildHeaderValue() {
      if (FrameOption.ALLOW_FROM == option) {
        if (origin == null || origin.isBlank()) {
          return FrameOption.DENY.getValue();
        }
        return option.getValue() + " " + origin;
      }
      return option.getValue();
    }
  }
}
