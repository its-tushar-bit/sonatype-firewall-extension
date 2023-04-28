/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.api.v2.service.JiraConfigurationListener;
import com.sonatype.insight.brain.api.v2.service.ProxyServerConfigurationListener;
import com.sonatype.insight.brain.api.v2.service.ReverseProxyAuthenticationConfigurationListener;
import com.sonatype.insight.brain.api.v2.service.SourceControlConfigurationListener;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ReverseProxyAuthenticationConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.jira.JiraConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.DefaultBranchMonitor;
import com.sonatype.insight.brain.git.PullRequestMonitor;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.jira.JiraConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler;
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeScheduler;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheProvider;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.AllowedIp;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.util.CollectionUtils;

@Named
@Singleton
public class Configuration
    implements ConfigurationListener, ReverseProxyAuthenticationConfigurationListener, JiraConfigurationListener,
               SourceControlConfigurationListener, ProxyServerConfigurationListener, TenantManaged
{
  private static final String BASE_URL_CONFIGURATION = "baseUrlConfiguration";

  private static final String PROXY_SERVER_CONFIGURATION = "proxyServerConfiguration";

  private static final String REVERSE_PROXY_AUTHENTICATION_CONFIGURATION = "reverseProxyAuthenticationConfiguration";

  private static final String JIRA_CONFIGURATION = "jiraConfiguration";

  private static final String SOURCE_CONTROL_CONFIGURATION = "sourceControlConfiguration";

  private final ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  private final ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO;

  private final JiraConfigurationDAO jiraConfigurationDAO;

  private final SourceControlConfigurationDAO sourceControlConfigurationDAO;

  private final ApiConfigurationService configurationService;

  private final Provider<List<HdsClient>> hdsClientsProvider;

  private final Provider<AsyncEventBus> asyncEventBusProvider;

  private final TaskScheduler taskScheduler;

  private final Provider<DefaultBranchMonitor> defaultBranchMonitorProvider;

  private final Provider<PullRequestMonitor> pullRequestMonitorProvider;

  private final ConfigurationMap configCache = new ConfigurationMap();

  private final Provider<ReleaseGraphCacheProvider> releaseGraphCacheProviderProvider;

  private final Provider<PolicyMonitorScheduler> policyMonitorSchedulerProvider;

  private final Provider<AutomaticQuarantineReleaseScheduler> automaticQuarantineReleaseSchedulerProvider;
  
  private final Provider<WaivedComponentUpgradeScheduler> waivedComponentUpgradeSchedulerProvider;

  @Inject
  public Configuration(
      ProxyServerConfigurationDAO proxyServerConfigurationDAO,
      ReverseProxyAuthenticationConfigurationDAO reverseProxyAuthenticationConfigurationDAO,
      JiraConfigurationDAO jiraConfigurationDAO,
      SourceControlConfigurationDAO sourceControlConfigurationDAO,
      ApiConfigurationService configurationService,
      Provider<List<HdsClient>> hdsClientsProvider,
      Provider<AsyncEventBus> asyncEventBusProvider,
      TaskScheduler taskScheduler,
      Provider<DefaultBranchMonitor> defaultBranchMonitorProvider,
      Provider<PullRequestMonitor> pullRequestMonitorProvider,
      Provider<ReleaseGraphCacheProvider> releaseGraphCacheProviderProvider,
      Provider<PolicyMonitorScheduler> policyMonitorSchedulerProvider,
      Provider<AutomaticQuarantineReleaseScheduler> automaticQuarantineReleaseSchedulerProvider,
      Provider<WaivedComponentUpgradeScheduler> waivedComponentUpgradeSchedulerProvider)
  {
    this.proxyServerConfigurationDAO = proxyServerConfigurationDAO;
    this.reverseProxyAuthenticationConfigurationDAO = reverseProxyAuthenticationConfigurationDAO;
    this.jiraConfigurationDAO = jiraConfigurationDAO;
    this.sourceControlConfigurationDAO = sourceControlConfigurationDAO;
    this.configurationService = configurationService;
    this.hdsClientsProvider = hdsClientsProvider;
    this.asyncEventBusProvider = asyncEventBusProvider;
    this.taskScheduler = taskScheduler;
    this.defaultBranchMonitorProvider = defaultBranchMonitorProvider;
    this.pullRequestMonitorProvider = pullRequestMonitorProvider;
    this.releaseGraphCacheProviderProvider = releaseGraphCacheProviderProvider;
    this.policyMonitorSchedulerProvider = policyMonitorSchedulerProvider;
    this.automaticQuarantineReleaseSchedulerProvider = automaticQuarantineReleaseSchedulerProvider;
    this.waivedComponentUpgradeSchedulerProvider = waivedComponentUpgradeSchedulerProvider;
    initializeValues();
  }

  @Override
  public void register() {
    initializeValues();
  }

  private void initializeValues() {
    updateValueByPropertyNames(CollectionUtils.asSet(
        SystemConfigurationProperty.ACCESS_ALLOWLIST,
        SystemConfigurationProperty.PURGE_SCAN_FILES,
        SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.FORCE_BASE_URL,
        SystemConfigurationProperty.HDS_URL,
        SystemConfigurationProperty.CDN_URL,
        SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES,
        SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX,
        SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
        SystemConfigurationProperty.CSRF_PROTECTION,
        SystemConfigurationProperty.USER_AGENT_SUFFIX,
        SystemConfigurationProperty.CSP_ENABLED,
        SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH,
        SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH,
        SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH,
        SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE,
        SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT,
        SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD,
        SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT,
        SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER,
        SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS,
        SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS,
        SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS,
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
        SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
        SystemConfigurationProperty.POLICY_MONITORING_HOUR,
        SystemConfigurationProperty.DB_BACKUP_DIR,
        SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE,
        SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED,
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING,
        SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST,
        SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX,
        SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT,
        SystemConfigurationProperty.BFS_REPOSITORIES,
        SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES,
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR,
        SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED,
        SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS)
    );
    configCache.putOrRemoveIfNull(PROXY_SERVER_CONFIGURATION, proxyServerConfigurationDAO.get());
    configCache.putOrRemoveIfNull(REVERSE_PROXY_AUTHENTICATION_CONFIGURATION,
        reverseProxyAuthenticationConfigurationDAO.get());
    configCache.putOrRemoveIfNull(JIRA_CONFIGURATION, jiraConfigurationDAO.get());
    configCache.putOrRemoveIfNull(SOURCE_CONTROL_CONFIGURATION, sourceControlConfigurationDAO.get());
  }

  private void updateValueByPropertyNames(Set<String> propertyNames) {
    Map<String, Object> result = configurationService.getConfigurationNoAuthz(propertyNames);
    updateBaseUrlConfigurationIfNeeded(result);
    for (Entry<String, Object> entry : result.entrySet()) {
      configCache.putOrRemoveIfNull(entry.getKey(), entry.getValue());
    }
  }

  private void updateBaseUrlConfigurationIfNeeded(Map<String, Object> result) {
    if (result.containsKey(SystemConfigurationProperty.BASE_URL) &&
        result.containsKey(SystemConfigurationProperty.FORCE_BASE_URL)) {
      BaseUrlConfiguration baseUrlConfiguration =
          new BaseUrlConfiguration((String) result.remove(SystemConfigurationProperty.BASE_URL),
              (boolean) result.remove(SystemConfigurationProperty.FORCE_BASE_URL));
      configCache.put(BASE_URL_CONFIGURATION, baseUrlConfiguration);
    }
  }

  @Override
  public void configurationChanged(Set<String> propertyNames) {
    Set<String> propertyNamesCopy = new HashSet<>(propertyNames);
    // Update baseUrl and forceBaseUrl together to make sure they're in sync
    if (propertyNamesCopy.contains(SystemConfigurationProperty.BASE_URL) ||
        propertyNamesCopy.contains(SystemConfigurationProperty.FORCE_BASE_URL)) {
      propertyNamesCopy.add(SystemConfigurationProperty.BASE_URL);
      propertyNamesCopy.add(SystemConfigurationProperty.FORCE_BASE_URL);
    }
    Integer currentPolicyMonitoringHour = getPolicyMonitoringHour();
    Integer currentAutomaticQuarantineReleaseTimeIntervalInMinutes =
        getAutomaticQuarantineReleaseTimeIntervalInMinutes();
    Integer currentWaivedComponentUpgradeInspectionHour = getWaivedComponentUpgradeInspectionHour();
    updateValueByPropertyNames(propertyNamesCopy);
    if (propertyNamesCopy.contains(SystemConfigurationProperty.HDS_URL) ||
        propertyNamesCopy.contains(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS) ||
        propertyNamesCopy.contains(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS)) {
      hdsClientsProvider.get().forEach(HdsClient::serverConfigurationChanged);
    }
    if (propertyNamesCopy.contains(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE)) {
      asyncEventBusProvider.get().setMaxPoolSize(getEventBusMaxThreadPoolSize());
    }
    if (propertyNamesCopy.contains(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE)) {
      releaseGraphCacheProviderProvider.get().initializeCache();
    }

    // Following prop changes deal with task scheduling so they are ignored if the scheduler is not initialized
    if (!taskScheduler.isSchedulerInitialized()) {
      return;
    }

    if (propertyNamesCopy.contains(SystemConfigurationProperty.POLICY_MONITORING_HOUR) &&
        !Objects.equals(currentPolicyMonitoringHour, getPolicyMonitoringHour())) {
      policyMonitorSchedulerProvider.get().schedulePolicyMonitoring();
    }
    if (propertyNamesCopy.contains(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES) &&
        !Objects.equals(currentAutomaticQuarantineReleaseTimeIntervalInMinutes,
            getAutomaticQuarantineReleaseTimeIntervalInMinutes())) {
      automaticQuarantineReleaseSchedulerProvider.get().scheduleAutomaticQuarantineRelease();
    }
    if (propertyNamesCopy.contains(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR) &&
        !Objects.equals(currentWaivedComponentUpgradeInspectionHour, getWaivedComponentUpgradeInspectionHour())) {
      waivedComponentUpgradeSchedulerProvider.get().scheduleWaivedComponentUpgradeInspection();
    }
  }

  @Override
  public void proxyServerConfigurationChanged() {
    configCache.putOrRemoveIfNull(PROXY_SERVER_CONFIGURATION, proxyServerConfigurationDAO.get());
    hdsClientsProvider.get().forEach(HdsClient::serverConfigurationChanged);
  }

  @Override
  public void reverseProxyAuthenticationConfigurationChanged() {
    configCache.putOrRemoveIfNull(REVERSE_PROXY_AUTHENTICATION_CONFIGURATION,
        reverseProxyAuthenticationConfigurationDAO.get());
  }

  @Override
  public void jiraConfigurationChanged() {
    configCache.putOrRemoveIfNull(JIRA_CONFIGURATION, jiraConfigurationDAO.get());
  }

  @Override
  public void sourceControlConfigurationChanged() {
    SourceControlConfiguration currentSourceControlConfiguration = getSourceControlConfigurationOrDefault();
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();
    configCache.putOrRemoveIfNull(SOURCE_CONTROL_CONFIGURATION, sourceControlConfiguration);
    if (sourceControlConfiguration == null) {
      sourceControlConfiguration = new SourceControlConfiguration();
    }
    updateDefaultBranchMonitoringIfNeeded(currentSourceControlConfiguration, sourceControlConfiguration);
    updatePullRequestMonitoringIfNeeded(currentSourceControlConfiguration, sourceControlConfiguration);
  }

  public String getPurgeScanFiles() {
    return configCache.get(SystemConfigurationProperty.PURGE_SCAN_FILES);
  }

  private void updateDefaultBranchMonitoringIfNeeded(
      SourceControlConfiguration currentSourceControlConfiguration,
      SourceControlConfiguration sourceControlConfiguration)
  {
    if (!taskScheduler.isSchedulerInitialized()) {
      return;
    }
    DefaultBranchMonitor defaultBranchMonitor = defaultBranchMonitorProvider.get();
    if (!taskScheduler.isTaskScheduled(defaultBranchMonitor) ||
        !Objects.equals(currentSourceControlConfiguration.getDefaultBranchMonitoringStartTime(),
            sourceControlConfiguration.getDefaultBranchMonitoringStartTime()) ||
        currentSourceControlConfiguration.getDefaultBranchMonitoringIntervalHours() !=
            sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours()) {
      defaultBranchMonitor.scheduleDefaultBranchMonitoring();
    }
  }

  private void updatePullRequestMonitoringIfNeeded(
      SourceControlConfiguration currentSourceControlConfiguration,
      SourceControlConfiguration sourceControlConfiguration)
  {
    if (!taskScheduler.isSchedulerInitialized()) {
      return;
    }
    PullRequestMonitor pullRequestMonitor = pullRequestMonitorProvider.get();
    if (!taskScheduler.isTaskScheduled(pullRequestMonitor) ||
        currentSourceControlConfiguration.getPullRequestMonitoringIntervalSeconds() !=
            sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()) {
      pullRequestMonitor.schedulePullRequestMonitor();
    }
  }

  public BaseUrlConfiguration getBaseUrlConfiguration() {
    return configCache.get(BASE_URL_CONFIGURATION);
  }

  public String getHdsUrl() {
    return configCache.get(SystemConfigurationProperty.HDS_URL);
  }

  public String getCdnUrl() {
    return configCache.get(SystemConfigurationProperty.CDN_URL);
  }

  public long getSupportReadLimitBytes() {
    return configCache.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES);
  }

  public String getSupportClusterLogFileRegex() {
    return configCache.get(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX);
  }

  public int getEventBusMaxThreadPoolSize() {
    return configCache.get(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE);
  }

  public boolean isAntiCsrfEnabled() {
    return configCache.get(SystemConfigurationProperty.CSRF_PROTECTION);
  }

  public void setAntiCsrfEnabled(boolean antiCsrfEnabled) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.CSRF_PROTECTION, antiCsrfEnabled);
    configurationService.updateAllClusterNodesFromConfiguration(
        Collections.singleton(SystemConfigurationProperty.CSRF_PROTECTION));
  }

  public String getUserAgentSuffix() {
    return configCache.get(SystemConfigurationProperty.USER_AGENT_SUFFIX);
  }

  public boolean isCspEnabled() {
    return configCache.get(SystemConfigurationProperty.CSP_ENABLED);
  }

  public boolean isBlockSemicolon() {
    return configCache.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH);
  }

  public void setBlockSemicolon(boolean blockSemicolon) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, blockSemicolon);
    configurationService.updateAllClusterNodesFromConfiguration(
        Collections.singleton(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH));
  }

  public boolean isBlockBackslash() {
    return configCache.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH);
  }

  public void setBlockBackslash(boolean blockBackslash) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, blockBackslash);
    configurationService.updateAllClusterNodesFromConfiguration(
        Collections.singleton(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH));
  }

  public boolean isBlockNonAscii() {
    return configCache.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH);
  }

  public void setBlockNonAscii(boolean blockNonAscii) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, blockNonAscii);
    configurationService.updateAllClusterNodesFromConfiguration(
        Collections.singleton(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH));
  }

  public ProxyServerConfiguration getProxyServerConfiguration() {
    return configCache.get(PROXY_SERVER_CONFIGURATION);
  }

  public ReverseProxyAuthenticationConfiguration getReverseProxyAuthenticationConfiguration() {
    return configCache.get(REVERSE_PROXY_AUTHENTICATION_CONFIGURATION);
  }

  public JiraConfiguration getJiraConfiguration() {
    return configCache.get(JIRA_CONFIGURATION);
  }

  public SourceControlConfiguration getSourceControlConfiguration() {
    return configCache.get(SOURCE_CONTROL_CONFIGURATION);
  }

  public SourceControlConfiguration getSourceControlConfigurationOrDefault() {
    SourceControlConfiguration sourceControlConfiguration = getSourceControlConfiguration();
    if (sourceControlConfiguration == null) {
      sourceControlConfiguration = new SourceControlConfiguration();
    }
    return sourceControlConfiguration;
  }

  public boolean isALPObservedLicenseDetectionEnabled() {
    return configCache.get(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED);
  }

  public void setALPObservedLicenseDetectionEnabled(boolean enableObservedLicenseDetection) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED,
        enableObservedLicenseDetection);
    configurationService.updateAllClusterNodesFromConfiguration(
        Collections.singleton(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED));
  }

  public int getReleaseGraphCacheSize() {
    return configCache.get(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE);
  }

  public int getLicenseLegalHdsRequestLimit() {
    return configCache.get(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT);
  }

  public int getMaxApplicationsToQueryOnDashboard() {
    return configCache.get(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD);
  }

  public int getMaxAdvancedSearchClauseCount() {
    return configCache.get(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
  }

  public String getAdvancedSearchCSVExportDelimiter() {
    return configCache.get(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
  }

  public int getConnectTimeoutInSeconds() {
    return configCache.get(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS);
  }

  public int getSocketTimeoutInSeconds() {
    return configCache.get(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS);
  }

  public int getReportTimeoutInSeconds() {
    return configCache.get(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS);
  }

  public boolean isNeedsAcknowledgementOfInitialDashboardFilter() {
    return configCache.get(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER);
  }

  public boolean isEnableDefaultPasswordWarning() {
    return configCache.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING);
  }

  public Integer getPolicyMonitoringHour() {
    return configCache.get(SystemConfigurationProperty.POLICY_MONITORING_HOUR);
  }

  public String getDbBackupDir() {
    return configCache.get(SystemConfigurationProperty.DB_BACKUP_DIR);
  }

  public String getWebhookSecretPassphrase() {
    return configCache.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE);
  }

  public boolean isExternalHyperlinksAllowed() {
    return configCache.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED);
  }

  public List<String> getFrameAncestorsAllowList() {
    return configCache.get(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST);
  }

  public List<AllowedIp> getAccessAllowlist() {
    List<AllowedIp> allowlist = configCache.get(SystemConfigurationProperty.ACCESS_ALLOWLIST);
    return allowlist == null ? new ArrayList<>() : allowlist;
  }

  public String getBfsArtifactoryExpiredTokenRegex() {
    return configCache.get(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX);
  }

  public String getBfsArtifactoryExpiredTokenEmail() {
    return configCache.get(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL);
  }

  public Integer getBfsComponentLimit() {
    return configCache.get(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT);
  }

  public String getBfsQueryRepositoriesList() {
    return configCache.get(SystemConfigurationProperty.BFS_REPOSITORIES);
  }

  public Integer getWaivedComponentUpgradeInspectionHour() {
    return configCache.get(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR);
  }

  public Integer getQuarantinedComponentReportExpirationTimeInHours() {
    return configCache.get(SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS);
  }

  public Map<String, String> getMatcherConfiguration() {
    Map<String, String> matcherConfiguration = new HashMap<>();
    for (Entry<String, Object> entry : configCache.get().entrySet()) {
      if (!entry.getKey().startsWith("matcherConfiguration")) {
        continue;
      }
      String[] split = entry.getKey().split("\\.");
      if (split.length != 2 || StringUtils.isBlank(split[1])) {
        continue;
      }
      matcherConfiguration.put(split[1], entry.getValue().toString());
    }
    return matcherConfiguration;
  }

  public Integer getAutomaticQuarantineReleaseTimeIntervalInMinutes() {
    return configCache.get(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES);
  }

  /**
   * Configuration map is encapsulated so that access can be controlled. As we can guarantee that config requests must
   * come through this implementation we only need to implement the tenant awareness inside ConfigurationMap rather than
   * each getter and setter with Configuration.
   * <p>
   * Note: SystemConfigurationPropertyDAO handles the mix of Global/Tenant-specific config. Meaning if a configuration
   * item does not exist for a specific tenant then the configuration from the Global tenant will be used instead.
   */
  private static class ConfigurationMap
  {
    private final TenantReference<Map<String, Object>> valueByPropertyName =
        new TenantReference<>(ConcurrentHashMap::new);

    public Map<String, Object> get() {
      return ImmutableMap.copyOf(valueByPropertyName.get());
    }

    public <T> T get(String property) {
      return (T) valueByPropertyName.get().get(property);
    }

    public void putOrRemoveIfNull(String key, Object value) {
      if (value == null) {
        valueByPropertyName.get().remove(key);
      }
      else {
        put(key, value);
      }
    }

    public void put(String key, Object value) {
      valueByPropertyName.get().put(key, value);
    }
  }

  /**
   * Configuration should be initialized for a tenant before any other job is initialized
   */
  @Override
  public int registrationPriority() {
    return 1;
  }
}
