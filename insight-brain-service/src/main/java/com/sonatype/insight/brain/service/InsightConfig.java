/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sonatype.insight.brain.eventbus.EventBusConfig;
import com.sonatype.insight.brain.jira.JiraConfig;
import com.sonatype.insight.brain.migration.MailConfigurationMigrator;
import com.sonatype.insight.brain.migration.ProxyServerConfigurationMigrator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.Configuration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsightConfig
    extends Configuration
{
  private static final Logger log = LoggerFactory.getLogger(InsightConfig.class);

  public static final String DEFAULT_BACKUP_DIR = "db-backup";

  {
    setServerFactory(new InsightDefaultServerFactory());
  }

  @JsonProperty
  private ProxyServerConfigurationMigrator.ProxyConfig proxy;

  @JsonProperty
  private MailConfigurationMigrator.MailConfig mail;

  @JsonProperty
  private String baseUrl;

  /**
   * When true, {@link DefaultBaseUrl#get()} will always return the configured {@link #baseUrl}.
   *
   * @since 1.41
   */
  @JsonProperty
  private boolean forceBaseUrl;

  @NotNull
  @JsonProperty
  private String hdsUrl = "https://clm.sonatype.com/";

  @NotNull
  @JsonProperty
  private String cdnUrl = "https://cdn.sonatype.com/";

  @NotNull
  @JsonProperty
  private String sonatypeWork = "sonatype-work/clm-server";

  /**
   * @since 1.99
   */
  @JsonProperty
  private String clusterDirectory;

  @NotNull
  @JsonProperty
  private SupportConfig support = new SupportConfig();

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
  private String dbBackupDir = DEFAULT_BACKUP_DIR;

  @NotNull
  @JsonProperty
  private int releaseGraphCacheSize = 1000;

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

  @NotNull
  @JsonProperty
  @Min(0)
  @Max(23)
  private int policyMonitoringHour = 0;

  /**
   * @since 1.16.0
   */
  @NotNull
  @JsonProperty
  private boolean csrfProtection = true;

  /**
   * @since 1.14.0
   */
  @NotNull
  @JsonProperty
  @Size(max = 128)
  @Pattern(regexp = "[^\\p{Cntrl}]*")
  private String userAgentSuffix = "";

  /**
   * @since 1.16.0
   */
  @NotNull
  @JsonProperty
  private ReverseProxyAuthenticationConfig reverseProxyAuthentication = new ReverseProxyAuthenticationConfig();

  /**
   * @since 1.21.0
   */
  @Valid
  @Nullable
  @JsonProperty
  private JiraConfig jira;

  /**
   * @since 1.20
   */
  @NotNull
  @JsonProperty
  private boolean exitOnFatalError = true;

  /**
   * @since 1.25.0
   */
  @NotNull
  @JsonProperty
  private String webhookSecretPassphrase = "^d1swM!FF&qQ";

  /**
   * @since 1.25.0
   */
  @NotNull
  @JsonProperty
  private EventBusConfig eventBus = new EventBusConfig();

  /**
   * @since 1.32
   */
  @NotNull
  @JsonProperty
  @Min(30)
  @Max(60 * 60)
  private int reportTimeoutInSeconds = 35 * 60;

  /**
   * If true, users must configure and acknowledge a filter before being able to see any data in the dashboard.
   *
   * @since 1.29
   */
  @NotNull
  @JsonProperty
  private boolean needsAcknowledgementOfInitialDashboardFilter = false;

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
   * Flag that signals consent by the user that the server can be upgraded to the new policy violation model.
   *
   * @since 1.45
   */
  @NotNull
  @JsonProperty
  @SuppressWarnings("checkstyle:MemberName")
  private boolean consentToUpgradeToVersion_1_45;

  /**
   * @since 1.47
   */
  @JsonProperty
  private boolean enableDefaultPasswordWarning = true;

  /**
   * @since 1.52
   */
  @JsonProperty
  private String licenseFile;

  /**
   * @since 1.55
   */
  @JsonProperty
  private boolean externalHyperlinksAllowed = true;

  /**
   * @since 1.59
   * This configuration disables the HTTP CSP header. It only exists because that header breaks the Geb functional
   * tests
   */
  @JsonProperty
  private boolean cspEnabled = true;

  /**
   * @since 1.73
   */
  @JsonProperty
  private SourceControlConfig sourceControl = new SourceControlConfig();

  /**
   * This section will be used for features that are enabled by default. If nothing is specified, or the feature flag is
   * set to {@code true}, the feature is enabled. To disable it, explicitly set it to {@code false}.
   * <p>
   * For example:
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
   * This section will be used for experimental features, which are disabled by default. If nothing is specified, or the
   * feature flag is set to {@code false}, the feature is disabled. To enable it, explicitly set it to {@code true}.
   * <p>
   * For example:
   * <pre>
   * # features are disabled by default - set to true if you want to enable them
   * experimentalFeatures:
   *   prLineCommenting: true
   * }
   * </pre>
   *
   * @since 1.90
   */
  @JsonProperty
  private Map<String, Boolean> experimentalFeatures;

  /**
   * This configuration blocks requests containing semicolons in the path to avoid malicious attacks.
   *
   * @since 1.98
   */
  @JsonProperty
  private boolean blockSemicolonInPath = true;

  /**
   * This configuration blocks requests containing backslash in the path to avoid malicious attacks.
   *
   * @since 1.98
   */

  @JsonProperty
  private boolean blockBackslashInPath = true;

  /**
   * This configuration blocks requests containing non-ASCII characters in the path to avoid malicious attacks.
   *
   * @since 1.98
   */
  @JsonProperty
  private boolean blockNonAsciiInPath = true;

  /**
   * This configuration limits the number of parallel requests for license data made to HDS for the Advanced Legal Pack.
   *
   * @since 1.101
   */
  @JsonProperty
  private int licenseLegalHdsRequestLimit = 50;

  /**
   * This configuration allows adjusting/tuning matcher behaviours based on specific customer needs.
   *
   * @since 1.101
   */
  @JsonProperty
  private Map<String, String> matcherConfiguration;

  public ProxyServerConfigurationMigrator.ProxyConfig getProxyConfig() {
    return proxy;
  }

  public MailConfigurationMigrator.MailConfig getMailConfig() {
    return mail;
  }

  public int getReleaseGraphCacheSize() {
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
  @ValidationMethod(message = "Cannot set sonatypeWork as the clusterDirectory.")
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

  public void setReleaseGraphCacheSize(int releaseGraphCacheSize) {
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
    if (baseUrl != null && !baseUrl.endsWith("/")) {
      this.baseUrl += '/';
    }
  }

  /**
   * @since 1.41
   */
  public boolean isForceBaseUrl() {
    return forceBaseUrl;
  }

  /**
   * @since 1.41
   */
  public void setForceBaseUrl(boolean forceBaseUrl) {
    this.forceBaseUrl = forceBaseUrl;
  }

  @JsonIgnore
  @ValidationMethod(message = "baseUrl is invalid")
  public boolean isValidBaseUrl() {
    try {
      String url = getBaseUrl();
      if (url != null) {
        new URL(url);
      }
      return true;
    }
    catch (Exception e) {
      log.error("Invalid baseUrl: {}", e.getMessage());
      return false;
    }
  }

  public String getCdnUrl() {
    return cdnUrl;
  }

  public void setCdnUrl(String cdnUrl) {
    this.cdnUrl = cdnUrl;
    if (cdnUrl != null && !cdnUrl.endsWith("/")) {
      this.cdnUrl += '/';
    }
  }

  @JsonIgnore
  @ValidationMethod(message = "cdnUrl is invalid")
  public boolean isValidCdnUrl() {
    try {
      String url = getCdnUrl();
      new URL(url);
      return true;
    }
    catch (Exception e) {
      log.error("Invalid cndUrl: {}", e.getMessage());
      return false;
    }
  }

  @JsonIgnore
  @ValidationMethod(message = "server.applicationConnectors cannot be empty")
  public boolean isValidApplicationConnectors() {
    return !((DefaultServerFactory) getServerFactory()).getApplicationConnectors().isEmpty();
  }

  /**
   * @since 1.8
   */
  public int getPolicyMonitoringHour() {
    return policyMonitoringHour;
  }

  /**
   * @since 1.8
   */
  public void setPolicyMonitoringHour(final int policyMonitoringHour) {
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
   *
   * @deprecated The support for anonymous client access was removed in 1.72.
   */
  @Deprecated
  public void setAnonymousClientAccessAllowed(@SuppressWarnings("unused") final boolean anonymousClientAccessAllowed) {
    log.warn("The support for anonymous client access was removed in Nexus IQ Server 72. "
        + "The anonymousClientAccessAllowed configuration option should be removed from the config yml file.");
  }

  public boolean isCsrfProtection() {
    return csrfProtection;
  }

  public void setCsrfProtection(boolean csrfProtection) {
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
  public File getDbBackupDir() {
    if (StringUtils.isBlank(dbBackupDir)) {
      dbBackupDir = DEFAULT_BACKUP_DIR;
    }

    File result = new File(dbBackupDir);
    if (!result.isAbsolute()) {
      result = new File(getSonatypeWork(), dbBackupDir);
    }

    return result;
  }

  /**
   * @deprecated Removed in 1.98.
   */
  @Deprecated
  public void setShowRootOrganization(@SuppressWarnings("unused") boolean showRootOrganization) {
    log.warn("The support for hiding the root organization was removed in Nexus IQ Server 98. "
        + "The showRootOrganization configuration option should be removed from the config yml file.");
  }

  void setDbBackupDir(String dbBackupDir) {
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
  @Nullable
  public JiraConfig getJiraConfig() {
    return jira;
  }

  /**
   * @since 1.21.0
   */
  public void setJiraConfig(@Nullable final JiraConfig jira) {
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
   *
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
  public boolean isNeedsAcknowledgementOfInitialDashboardFilter() {
    return needsAcknowledgementOfInitialDashboardFilter;
  }

  /**
   * @since 1.29
   */
  public void setNeedsAcknowledgementOfInitialDashboardFilter(boolean needsAcknowledgementOfInitialDashboardFilter) {
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
  public void setImportRefrencePoliciesFromHDS(boolean importReferencePoliciesFromHDS) {
    this.importReferencePoliciesFromHDS = importReferencePoliciesFromHDS;
  }

  /**
   * The time in seconds that IQ server is expected to wait for the report before timing out.
   *
   * @since 1.32
   */
  public int getReportTimeoutInSeconds() {
    return reportTimeoutInSeconds;
  }

  /**
   * @since 1.32
   */
  public void setReportTimeoutInSeconds(final int reportTimeoutInSeconds) {
    this.reportTimeoutInSeconds = reportTimeoutInSeconds;
  }

  public boolean isConsentToUpgradeToVersion_1_45() {
    return consentToUpgradeToVersion_1_45;
  }

  @SuppressWarnings("checkstyle:ParameterName")
  public void setConsentToUpgradeToVersion_1_45(boolean consentToUpgradeToVersion_1_45) {
    this.consentToUpgradeToVersion_1_45 = consentToUpgradeToVersion_1_45;
  }

  public boolean isEnableDefaultPasswordWarning() {
    return enableDefaultPasswordWarning;
  }

  public void setEnableDefaultPasswordWarning(boolean enableDefaultPasswordWarning) {
    this.enableDefaultPasswordWarning = enableDefaultPasswordWarning;
  }

  public String getLicenseFile() {
    return licenseFile;
  }

  public void setLicenseFile(final String licenseFile) {
    this.licenseFile = licenseFile;
  }

  public boolean isExternalHyperlinksAllowed() {
    return externalHyperlinksAllowed;
  }

  public void setExternalHyperlinksAllowed(final boolean externalHyperlinksAllowed) {
    this.externalHyperlinksAllowed = externalHyperlinksAllowed;
  }

  public boolean isCspEnabled() {
    return cspEnabled;
  }

  public void setCspEnabled(final boolean cspEnabled) {
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
  @NotNull
  @Min(5)
  @Max(60 * 60)  // 1 hour
  private int connectTimeoutInSeconds = 20;

  /**
   * This uses a generous default value to account for batched component data requests that are known to occasionally
   * take ~1 minute.
   * 
   * @since 1.101
   */
  @JsonProperty
  @NotNull
  @Min(5)
  @Max(60 * 60) // 1 hour
  private int socketTimeoutInSeconds = 60 * 3;

  /**
   * @since 1.114
   */
  @JsonProperty
  @NotNull
  @Min(0)
  private int pullRequestDetailsUpdateIntervalInSeconds = 60;

  public int getConnectTimeoutInSeconds() {
    return connectTimeoutInSeconds;
  }

  public void setConnectTimeoutInSeconds(int connectTimeoutInSeconds) {
    this.connectTimeoutInSeconds = connectTimeoutInSeconds;
  }

  public int getSocketTimeoutInSeconds() {
    return socketTimeoutInSeconds;
  }

  public void setSocketTimeoutInSeconds(int socketTimeoutInSeconds) {
    this.socketTimeoutInSeconds = socketTimeoutInSeconds;
  }

  public SourceControlConfig getSourceControl() {
    // Ensure the sonatypeWorkDir is set
    sourceControl.setSonatypeWorkDir(getSonatypeWork());

    return sourceControl;
  }

  public void setSourceControl(final SourceControlConfig sourceControl) {
    if (sourceControl == null) {
      this.sourceControl = new SourceControlConfig();
    }
    else {
      this.sourceControl = sourceControl;
    }
  }

  /**
   * Returns a map of declared feature flags and their states i.e. enabled or disabled.
   * @see InsightConfig#features for details on how feature flags are specified
   */
  public Map<String, Boolean> getFeatures() {
    return features;
  }

  /**
   * Returns {@code true} if a feature flag, identified by name, is enabled; returns {@code false} otherwise.
   * <p>If the feature flag is not explicitly declared it is considered enabled by default.
   * @see InsightConfig#features for details on how feature flags are specified
   */
  @VisibleForTesting
  boolean isFeatureEnabled(String feature) {
    return features == null || !features.containsKey(feature) || features.containsKey(feature) && features.get(feature);
  }

  /**
   * Returns {@code true} if a feature flag is enabled; returns {@code false} otherwise.
   * <p>If the feature flag is not explicitly declared it is considered enabled by default.
   * @see InsightConfig#features for details on how feature flags are specified
   */
  public boolean isFeatureEnabled(Feature feature) {
    return isFeatureEnabled(feature.flag);
  }

  public void setFeatures(final Map<String, Boolean> features) {
    this.features = features;
  }

  /**
   * Returns a map of declared experimental feature flags and their states i.e. enabled or disabled.
   * @see InsightConfig#experimentalFeatures for details on how experimental feature flags are specified
   */
  public Map<String, Boolean> getExperimentalFeatures() {
    return experimentalFeatures;
  }

  /**
   * Returns {@code true} if an experimental feature flag, identified by name, is enabled; returns {@code false}
   * otherwise.
   * <p>If the experimental feature flag is not explicitly declared it is considered disabled by default.
   * @see InsightConfig#experimentalFeatures for details on how experimental feature flags are specified
   */
  public boolean isExperimentalFeatureEnabled(String feature) {
    return experimentalFeatures != null && experimentalFeatures.containsKey(feature) &&
        experimentalFeatures.get(feature);
  }

  /**
   * Returns {@code true} if an experimental feature flag is enabled; returns {@code false} otherwise.
   * <p>If the experimental feature flag is not explicitly declared it is considered disabled by default.
   * @see InsightConfig#experimentalFeatures for details on how experimental feature flags are specified
   */
  public boolean isExperimentalFeatureEnabled(Feature feature) {
    return isExperimentalFeatureEnabled(feature.flag);
  }

  public void setExperimentalFeatures(final Map<String, Boolean> experimentalFeatures) {
    this.experimentalFeatures = experimentalFeatures;
  }

  public boolean isBlockSemicolonInPath() {
    return blockSemicolonInPath;
  }

  public void setBlockSemicolonInPath(boolean blockSemicolonInPath) {
    this.blockSemicolonInPath = blockSemicolonInPath;
  }

  public boolean isBlockBackslashInPath() {
    return blockBackslashInPath;
  }

  public void setBlockBackslashInPath(boolean blockBackslashInPath) {
    this.blockBackslashInPath = blockBackslashInPath;
  }

  public boolean isBlockNonAsciiInPath() {
    return blockNonAsciiInPath;
  }

  public void setBlockNonAsciiInPath(boolean blockNonAsciiInPath) {
    this.blockNonAsciiInPath = blockNonAsciiInPath;
  }

  public int getLicenseLegalHdsRequestLimit() {
    return licenseLegalHdsRequestLimit;
  }

  public void setLicenseLegalHdsRequestLimit(int licenseLegalHdsRequestLimit) {
    this.licenseLegalHdsRequestLimit = licenseLegalHdsRequestLimit;
  }

  public Map<String, String> getMatcherConfiguration() {
    return matcherConfiguration;
  }

  public void setMatcherConfiguration(Map<String, String> matcherConfiguration) {
    this.matcherConfiguration = matcherConfiguration;
  }

  public enum Feature
  {
    PR_COMMENTING("prCommenting"), //
    PR_LINE_COMMENTING("prLineCommenting"), //
    PR_COMMENT_MONITORING("prCommentMonitoring"), //
    CODE_INSIGHTS("codeInsights"), //
    SCM_ONBOARDING("scmOnboarding"), //
    FIREWALL_AUTO_UNQUARANTINE("firewallAutoUnquarantine"), //
    INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS("internalSourceControlPolicyEvaluations"), //
    DEPENDENCY_DATA_IN_API("dependencyDataInApi"), //
    COMPONENT_SEARCH_API_WITH_INNERSOURCE("componentSearchApiWithInnerSource");

    private String flag;

    Feature(final String flag) {
      this.flag = flag;
    }

    public String getFlag() {
      return flag;
    }
  }

  public int getPullRequestDetailsUpdateIntervalInSeconds() {
    return pullRequestDetailsUpdateIntervalInSeconds;
  }

  public void setPullRequestDetailsUpdateIntervalInSeconds(int pullRequestDetailsUpdateIntervalInSeconds) {
    this.pullRequestDetailsUpdateIntervalInSeconds = pullRequestDetailsUpdateIntervalInSeconds;
  }
}
