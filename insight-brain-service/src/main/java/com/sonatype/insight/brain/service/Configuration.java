/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

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
import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.telemetry.HistoricalPolicyViolationTelemetryTask;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;

@Named
@Singleton
public class Configuration
    implements ConfigurationListener, ReverseProxyAuthenticationConfigurationListener, JiraConfigurationListener,
               SourceControlConfigurationListener, ProxyServerConfigurationListener, TenantManaged, BaseUrlProvider
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

  private final Provider<HistoricalPolicyViolationTelemetryTask> historicalPolicyViolationTelemetryTaskProvider;

  private final Provider<AutomaticQuarantineReleaseScheduler> automaticQuarantineReleaseSchedulerProvider;

  private final Provider<WaivedComponentUpgradeScheduler> waivedComponentUpgradeSchedulerProvider;

  private final TenantUtil tenantUtil;

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
      Provider<WaivedComponentUpgradeScheduler> waivedComponentUpgradeSchedulerProvider,
      Provider<HistoricalPolicyViolationTelemetryTask> historicalPolicyViolationTelemetryTaskProvider,
      TenantUtil tenantUtil)
  {
    this.historicalPolicyViolationTelemetryTaskProvider = historicalPolicyViolationTelemetryTaskProvider;
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
    this.tenantUtil = tenantUtil;
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
        SystemConfigurationProperty.SAAS_POLICY_MONITOR_POOL_SIZE,
        SystemConfigurationProperty.SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE,
        SystemConfigurationProperty.SOURCE_CONTROL_IMPORT_POOL_SIZE,
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
        SystemConfigurationProperty.MAX_CONCURRENT_TENANT_INDEX_CREATION,
        SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER,
        SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS,
        SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS,
        SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS,
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
        SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
        SystemConfigurationProperty.POLICY_MONITORING_HOUR,
        SystemConfigurationProperty.DB_BACKUP_DIR,
        SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE,
        SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS,
        SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED,
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING,
        SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST,
        SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX,
        SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT,
        SystemConfigurationProperty.BFS_ARTIFACTORY_AQL_BATCH_SIZE,
        SystemConfigurationProperty.BFS_REPOSITORIES,
        SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES,
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR,
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED,
        SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED,
        SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS,
        SystemConfigurationProperty.ENTERPRISE_REPORTING_VERSION_CACHE_EXPIRATION_IN_MINUTES,
        SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED,
        SystemConfigurationProperty.API_ACCESS_ALLOW_LIST,
        SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION,
        SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID,
        SystemConfigurationProperty.SBOM_BINARY_SCANNING,
        SystemConfigurationProperty.MALWARE_DEFENSE_API_MAX_COMPONENTS,
        SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI,
        SystemConfigurationProperty.SBOM_POLICIES,
        SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR,
        SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_COMPONENTS,
        SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_BATCH_SIZE,
        SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_TASK_PERIOD,
        SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_EVENTS,
        SystemConfigurationProperty.ALP_FOR_SBOM_MANAGER,
        SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_API,
        SystemConfigurationProperty.CONTAINER_IMAGES_EVAL_ENABLED,
        SystemConfigurationProperty.ZSCALER_UPDATE_TASK_PERIOD,
        SystemConfigurationProperty.ZSCALER_MAX_URLS_PER_CATEGORY,
        SystemConfigurationProperty.ZSCALER,
        SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP,
        SystemConfigurationProperty.EPSS_DATA,
        SystemConfigurationProperty.INTEGRATIONS_SUPPORTED_VERSION_COUNT,
        SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS,
        SystemConfigurationProperty.MALICIOUS_URLS_PARTNER_ACCESS
        )
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
    Integer currentHistoricalPolicyViolationTelemetryHour = getHistoricalPolicyViolationTelemetryHour();
    Integer currentAutomaticQuarantineReleaseTimeIntervalInMinutes =
        getAutomaticQuarantineReleaseTimeIntervalInMinutes();
    Integer currentWaivedComponentUpgradeInspectionHour = getWaivedComponentUpgradeInspectionHour();
    boolean isWaivedComponentUpgradeMonitoringEnabled = getWaivedComponentUpgradeMonitoringEnabled();
    updateValueByPropertyNames(propertyNamesCopy);
    hdsUrlAndTimeoutsServerConfigurationChanged(propertyNamesCopy);
    eventBusMaxThreadPoolSizeSetMaxPoolSize(propertyNamesCopy);
    releaseGraphCacheSizeInitializeCache(propertyNamesCopy);
    if (!taskScheduler.isSchedulerInitialized()) {
      return;
    }
    policyMonitoringHourSchedulePolicyMonitoring(propertyNamesCopy, currentPolicyMonitoringHour);
    historicalPolicyViolationTelemetryScheduleHour(propertyNamesCopy, currentHistoricalPolicyViolationTelemetryHour);
    automaticQuarantineReleaseTimeIntervalInMinutesScheduleAutomaticQuarantineRelease(propertyNamesCopy,
        currentAutomaticQuarantineReleaseTimeIntervalInMinutes);
    waivedComponentUpgradeInspectionHourScheduleWaivedComponentUpgradeInspection(propertyNamesCopy,
        isWaivedComponentUpgradeMonitoringEnabled, currentWaivedComponentUpgradeInspectionHour);
    waivedComponentUpgradeMonitoringEnabledScheduleWaivedComponentUpgradeInspectionOrDeregister(propertyNamesCopy,
        isWaivedComponentUpgradeMonitoringEnabled );
  }

  private void hdsUrlAndTimeoutsServerConfigurationChanged(Set<String> propertyNamesCopy) {
    filterAndAction(propertyNamesCopy,
        prop -> prop.equals(SystemConfigurationProperty.HDS_URL) ||
            prop.equals(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS) ||
            prop.equals(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS),
        prop -> hdsClientsProvider.get().forEach(HdsClient::serverConfigurationChanged)
    );
  }

  private void eventBusMaxThreadPoolSizeSetMaxPoolSize(Set<String> propertyNamesCopy) {
    filterAndAction(propertyNamesCopy,
        prop -> prop.equals(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE),
        prop -> asyncEventBusProvider.get().setMaxPoolSize(getEventBusMaxThreadPoolSize())
    );
  }

  private void releaseGraphCacheSizeInitializeCache(Set<String> propertyNamesCopy) {
    filterAndAction(propertyNamesCopy,
        prop -> prop.equals(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE),
        prop -> releaseGraphCacheProviderProvider.get().initializeCache()
    );
  }

  private void policyMonitoringHourSchedulePolicyMonitoring(Set<String> propertyNamesCopy,
                                                            Integer currentPolicyMonitoringHour)
  {
    filterAndAction(propertyNamesCopy,
        prop -> prop.equals(SystemConfigurationProperty.POLICY_MONITORING_HOUR) &&
            !Objects.equals(currentPolicyMonitoringHour, getPolicyMonitoringHour()),
        prop -> policyMonitorSchedulerProvider.get().schedulePolicyMonitoring()
    );
  }

  private void historicalPolicyViolationTelemetryScheduleHour(
      Set<String> propertyNamesCopy, Integer currentHistoricalPolicyViolationTelemetryHour)
  {
    filterAndAction(propertyNamesCopy,
        prop -> prop.equals(SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR) &&
            !Objects.equals(currentHistoricalPolicyViolationTelemetryHour, getHistoricalPolicyViolationTelemetryHour()),
        prop -> historicalPolicyViolationTelemetryTaskProvider.get().scheduleHistoricalPolicyViolationTelemetryTask());
  }

  private void automaticQuarantineReleaseTimeIntervalInMinutesScheduleAutomaticQuarantineRelease(
      Set<String> propertyNamesCopy, Integer currentAutomaticQuarantineReleaseTimeIntervalInMinutes)
  {
    filterAndAction(propertyNamesCopy,
        prop -> prop.equals(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES)
            && !Objects.equals(currentAutomaticQuarantineReleaseTimeIntervalInMinutes,
            getAutomaticQuarantineReleaseTimeIntervalInMinutes()),
        prop -> automaticQuarantineReleaseSchedulerProvider.get().scheduleAutomaticQuarantineRelease()
    );
  }

  private void waivedComponentUpgradeInspectionHourScheduleWaivedComponentUpgradeInspection(
      Set<String> propertyNamesCopy, boolean isWaivedComponentUpgradeMonitoringEnabled,
      Integer currentWaivedComponentUpgradeInspectionHour)
  {
    filterAndAction(propertyNamesCopy,
        prop -> prop.equals(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR) &&
            isWaivedComponentUpgradeMonitoringEnabled && !Objects.equals(currentWaivedComponentUpgradeInspectionHour,
            getWaivedComponentUpgradeInspectionHour()),
        prop -> waivedComponentUpgradeSchedulerProvider.get().scheduleWaivedComponentUpgradeInspection()
    );
  }

  private void waivedComponentUpgradeMonitoringEnabledScheduleWaivedComponentUpgradeInspectionOrDeregister(
      Set<String> propertyNamesCopy, boolean isWaivedComponentUpgradeMonitoringEnabled)
  {
    filterAndAction(propertyNamesCopy,
        prop -> prop.equals(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED) &&
            !Objects.equals(isWaivedComponentUpgradeMonitoringEnabled, getWaivedComponentUpgradeMonitoringEnabled()),
        prop -> {
          Runnable action = getWaivedComponentUpgradeMonitoringEnabled()
              ? () -> waivedComponentUpgradeSchedulerProvider.get().scheduleWaivedComponentUpgradeInspection()
              : () -> waivedComponentUpgradeSchedulerProvider.get().deregister();
          action.run();
        }
    );
  }

  private void filterAndAction(Set<String> propertyNames,
                                            Predicate<String> filterPredicate, Consumer<String> action)
  {
    propertyNames.stream()
        .filter(filterPredicate)
        .findAny()
        .ifPresent(action);
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

  @Override
  public String getBaseUrl() {
    return null != getBaseUrlConfiguration() ? getBaseUrlConfiguration().getBaseUrl() : null;
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

  public int getSourceControlEventProcessorPoolSize() {
    return configCache.get(SystemConfigurationProperty.SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE);
  }

  public int getSourceControlImportPoolSize() {
    return configCache.get(SystemConfigurationProperty.SOURCE_CONTROL_IMPORT_POOL_SIZE);
  }

  public boolean isAntiCsrfEnabled() {
    return configCache.get(SystemConfigurationProperty.CSRF_PROTECTION);
  }

  public void setAntiCsrfEnabled(boolean antiCsrfEnabled) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.CSRF_PROTECTION, antiCsrfEnabled);
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
  }

  public boolean isBlockBackslash() {
    return configCache.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH);
  }

  public void setBlockBackslash(boolean blockBackslash) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, blockBackslash);
  }

  public boolean isBlockNonAscii() {
    return configCache.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH);
  }

  public void setBlockNonAscii(boolean blockNonAscii) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, blockNonAscii);
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

  public int getMaxConcurrentTenantIndexCreation() {
    return configCache.get(SystemConfigurationProperty.MAX_CONCURRENT_TENANT_INDEX_CREATION);
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

  public Integer getHistoricalPolicyViolationTelemetryHour() {
    return configCache.get(SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR);
  }

  public String getDbBackupDir() {
    return configCache.get(SystemConfigurationProperty.DB_BACKUP_DIR);
  }

  public String getWebhookSecretPassphrase() {
    return FIPSModeDetector.isEnabled() ?
        configCache.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS) :
        configCache.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE);
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

  public List<String> getApiAccessAllowList() {
    List<String> allowlist = configCache.get(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST);
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

  public Integer getBfsArtifactoryAqlBatchSize() {
    return configCache.get(SystemConfigurationProperty.BFS_ARTIFACTORY_AQL_BATCH_SIZE);
  }

  public String getBfsQueryRepositoriesList() {
    return configCache.get(SystemConfigurationProperty.BFS_REPOSITORIES);
  }

  public Integer getWaivedComponentUpgradeInspectionHour() {
    return configCache.get(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR);
  }

  public boolean getWaivedComponentUpgradeMonitoringEnabled() {
    return configCache.get(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED);
  }

  public Integer getQuarantinedComponentReportExpirationTimeInHours() {
    return configCache.get(SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS);
  }

  public Integer getEnterpriseReportingVersionCacheExpirationInMinutes() {
    return configCache.get(SystemConfigurationProperty.ENTERPRISE_REPORTING_VERSION_CACHE_EXPIRATION_IN_MINUTES);
  }

  public Integer getSaasPolicyMonitorPoolSize() {
    if (tenantUtil.isSingleTenant()) {
      return null;
    }
    return configCache.get(SystemConfigurationProperty.SAAS_POLICY_MONITOR_POOL_SIZE);
  }

  public String getSuccessMetricsStageId() {
    return configCache.get(SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID);
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

  public boolean getAdvanceReportingInsightsEnabled() {
    return configCache.get(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED);
  }

  public Integer getMalwareDefenseApiMaxComponents() {
    return configCache.get(SystemConfigurationProperty.MALWARE_DEFENSE_API_MAX_COMPONENTS);
  }

  public Integer getComponentChangeDetectionMaxComponents() {
    return configCache.get(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_COMPONENTS);
  }

  public Integer getComponentChangeDetectionBatchSize() {
    return configCache.get(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_BATCH_SIZE);
  }

  public Integer getComponentChangeDetectionTaskPeriod() {
    return configCache.get(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_TASK_PERIOD);
  }

  public Integer getComponentChangeDetectionMaxEvents() {
    return configCache.get(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_EVENTS);
  }

  public Integer getZScalerUpdateTaskPeriod() {
    return configCache.get(SystemConfigurationProperty.ZSCALER_UPDATE_TASK_PERIOD);
  }

  public Integer getZScalerMaxUrlsPerCategory() {
    return configCache.get(SystemConfigurationProperty.ZSCALER_MAX_URLS_PER_CATEGORY);
  }

  public Integer getIntegrationsSupportedVersionCount() {
    return configCache.get(SystemConfigurationProperty.INTEGRATIONS_SUPPORTED_VERSION_COUNT);
  }

  public Integer getUserTokenDefaultExpirationDays() {
    return configCache.get(SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS);
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
