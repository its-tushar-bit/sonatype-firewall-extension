/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

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
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheProvider;
import com.sonatype.insight.brain.scheduler.TaskScheduler;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.util.CollectionUtils;

@Named
@Singleton
public class Configuration
    implements ConfigurationListener, ReverseProxyAuthenticationConfigurationListener, JiraConfigurationListener,
               SourceControlConfigurationListener, ProxyServerConfigurationListener
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

  private final Map<String, Object> valueByPropertyName = new ConcurrentHashMap<>();

  private final Provider<ReleaseGraphCacheProvider> releaseGraphCacheProviderProvider;
  
  private final Provider<PolicyMonitorScheduler> policyMonitorSchedulerProvider;

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
      Provider<PolicyMonitorScheduler> policyMonitorSchedulerProvider)
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
    initializeValues();
  }

  private void initializeValues() {
    updateValueByPropertyNames(CollectionUtils.asSet(
        SystemConfigurationProperty.BASE_URL,
        SystemConfigurationProperty.FORCE_BASE_URL,
        SystemConfigurationProperty.HDS_URL,
        SystemConfigurationProperty.CDN_URL,
        SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES,
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
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)
    );
    putOrRemoveIfNull(PROXY_SERVER_CONFIGURATION, proxyServerConfigurationDAO.get());
    putOrRemoveIfNull(REVERSE_PROXY_AUTHENTICATION_CONFIGURATION, reverseProxyAuthenticationConfigurationDAO.get());
    putOrRemoveIfNull(JIRA_CONFIGURATION, jiraConfigurationDAO.get());
    putOrRemoveIfNull(SOURCE_CONTROL_CONFIGURATION, sourceControlConfigurationDAO.get());
  }

  private void putOrRemoveIfNull(String key, Object value) {
    if (value == null) {
      valueByPropertyName.remove(key);
    }
    else {
      valueByPropertyName.put(key, value);
    }
  }

  private void updateValueByPropertyNames(Set<String> propertyNames) {
    Map<String, Object> result = configurationService.getConfigurationNoAuthz(propertyNames);
    updateBaseUrlConfigurationIfNeeded(result);
    for (Entry<String, Object> entry : result.entrySet()) {
      putOrRemoveIfNull(entry.getKey(), entry.getValue());
    }
  }

  private void updateBaseUrlConfigurationIfNeeded(Map<String, Object> result) {
    if (result.containsKey(SystemConfigurationProperty.BASE_URL) &&
        result.containsKey(SystemConfigurationProperty.FORCE_BASE_URL)) {
      BaseUrlConfiguration baseUrlConfiguration =
          new BaseUrlConfiguration((String) result.remove(SystemConfigurationProperty.BASE_URL),
              (boolean) result.remove(SystemConfigurationProperty.FORCE_BASE_URL));
      valueByPropertyName.put(BASE_URL_CONFIGURATION, baseUrlConfiguration);
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
    if (propertyNamesCopy.contains(SystemConfigurationProperty.POLICY_MONITORING_HOUR) &&
        !Objects.equals(currentPolicyMonitoringHour, getPolicyMonitoringHour())) {
      if (!taskScheduler.isSchedulerInitialized()) {
        return;
      }
      policyMonitorSchedulerProvider.get().schedulePolicyMonitoring();
    }
  }

  @Override
  public void proxyServerConfigurationChanged() {
    putOrRemoveIfNull(PROXY_SERVER_CONFIGURATION, proxyServerConfigurationDAO.get());
    hdsClientsProvider.get().forEach(HdsClient::serverConfigurationChanged);
  }

  @Override
  public void reverseProxyAuthenticationConfigurationChanged() {
    putOrRemoveIfNull(REVERSE_PROXY_AUTHENTICATION_CONFIGURATION, reverseProxyAuthenticationConfigurationDAO.get());
  }

  @Override
  public void jiraConfigurationChanged() {
    putOrRemoveIfNull(JIRA_CONFIGURATION, jiraConfigurationDAO.get());
  }

  @Override
  public void sourceControlConfigurationChanged() {
    SourceControlConfiguration currentSourceControlConfiguration = getSourceControlConfigurationOrDefault();
    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();
    putOrRemoveIfNull(SOURCE_CONTROL_CONFIGURATION, sourceControlConfiguration);
    if (sourceControlConfiguration == null) {
      sourceControlConfiguration = new SourceControlConfiguration();
    }
    updateDefaultBranchMonitoringIfNeeded(currentSourceControlConfiguration, sourceControlConfiguration);
    updatePullRequestMonitoringIfNeeded(currentSourceControlConfiguration, sourceControlConfiguration);
  }

  private void updateDefaultBranchMonitoringIfNeeded(
      SourceControlConfiguration currentSourceControlConfiguration,
      SourceControlConfiguration sourceControlConfiguration)
  {
    if (!taskScheduler.isSchedulerInitialized()) {
      return;
    }
    if (!taskScheduler.isTaskScheduled(DefaultBranchMonitor.TASK_NAME) ||
        !Objects.equals(currentSourceControlConfiguration.getDefaultBranchMonitoringStartTime(),
            sourceControlConfiguration.getDefaultBranchMonitoringStartTime()) ||
        currentSourceControlConfiguration.getDefaultBranchMonitoringIntervalHours() !=
            sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours()) {
      defaultBranchMonitorProvider.get().scheduleDefaultBranchMonitoring();
    }
  }

  private void updatePullRequestMonitoringIfNeeded(
      SourceControlConfiguration currentSourceControlConfiguration,
      SourceControlConfiguration sourceControlConfiguration)
  {
    if (!taskScheduler.isSchedulerInitialized()) {
      return;
    }
    if (!taskScheduler.isTaskScheduled(PullRequestMonitor.TASK_NAME) ||
        currentSourceControlConfiguration.getPullRequestMonitoringIntervalSeconds() !=
            sourceControlConfiguration.getPullRequestMonitoringIntervalSeconds()) {
      pullRequestMonitorProvider.get().schedulePullRequestMonitor();
    }
  }

  public BaseUrlConfiguration getBaseUrlConfiguration() {
    return (BaseUrlConfiguration) valueByPropertyName.get(BASE_URL_CONFIGURATION);
  }

  public String getHdsUrl() {
    return (String) valueByPropertyName.get(SystemConfigurationProperty.HDS_URL);
  }

  public String getCdnUrl() {
    return (String) valueByPropertyName.get(SystemConfigurationProperty.CDN_URL);
  }

  public long getSupportReadLimitBytes() {
    return (long) valueByPropertyName.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES);
  }

  public int getEventBusMaxThreadPoolSize() {
    return (int) valueByPropertyName.get(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE);
  }

  public boolean isAntiCsrfEnabled() {
    return (boolean) valueByPropertyName.get(SystemConfigurationProperty.CSRF_PROTECTION);
  }

  public void setAntiCsrfEnabled(boolean antiCsrfEnabled) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.CSRF_PROTECTION, antiCsrfEnabled);
    configurationService.updateAllClusterNodesFromConfiguration(
        Collections.singleton(SystemConfigurationProperty.CSRF_PROTECTION));
  }

  public String getUserAgentSuffix() {
    return (String) valueByPropertyName.get(SystemConfigurationProperty.USER_AGENT_SUFFIX);
  }

  public boolean isCspEnabled() {
    return (boolean) valueByPropertyName.get(SystemConfigurationProperty.CSP_ENABLED);
  }

  public boolean isBlockSemicolon() {
    return (boolean) valueByPropertyName.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH);
  }

  public void setBlockSemicolon(boolean blockSemicolon) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, blockSemicolon);
    configurationService.updateAllClusterNodesFromConfiguration(
        Collections.singleton(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH));
  }

  public boolean isBlockBackslash() {
    return (boolean) valueByPropertyName.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH);
  }

  public void setBlockBackslash(boolean blockBackslash) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, blockBackslash);
    configurationService.updateAllClusterNodesFromConfiguration(
        Collections.singleton(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH));
  }

  public boolean isBlockNonAscii() {
    return (boolean) valueByPropertyName.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH);
  }

  public void setBlockNonAscii(boolean blockNonAscii) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, blockNonAscii);
    configurationService.updateAllClusterNodesFromConfiguration(
        Collections.singleton(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH));
  }

  public ProxyServerConfiguration getProxyServerConfiguration() {
    return (ProxyServerConfiguration) valueByPropertyName.get(PROXY_SERVER_CONFIGURATION);
  }

  public ReverseProxyAuthenticationConfiguration getReverseProxyAuthenticationConfiguration() {
    return (ReverseProxyAuthenticationConfiguration) valueByPropertyName.get(
        REVERSE_PROXY_AUTHENTICATION_CONFIGURATION);
  }

  public JiraConfiguration getJiraConfiguration() {
    return (JiraConfiguration) valueByPropertyName.get(JIRA_CONFIGURATION);
  }

  public SourceControlConfiguration getSourceControlConfiguration() {
    return (SourceControlConfiguration) valueByPropertyName.get(SOURCE_CONTROL_CONFIGURATION);
  }

  public SourceControlConfiguration getSourceControlConfigurationOrDefault() {
    SourceControlConfiguration sourceControlConfiguration = getSourceControlConfiguration();
    if (sourceControlConfiguration == null) {
      sourceControlConfiguration = new SourceControlConfiguration();
    }
    return sourceControlConfiguration;
  }

  public int getReleaseGraphCacheSize() {
    return (int) valueByPropertyName.get(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE);
  }

  public int getLicenseLegalHdsRequestLimit() {
    return (int) valueByPropertyName.get(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT);
  }

  public int getMaxApplicationsToQueryOnDashboard() {
    return (int) valueByPropertyName.get(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD);
  }

  public int getMaxAdvancedSearchClauseCount() {
    return (int) valueByPropertyName.get(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT);
  }

  public String getAdvancedSearchCSVExportDelimiter() {
    return (String) valueByPropertyName.get(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER);
  }

  public int getConnectTimeoutInSeconds() {
    return (int) valueByPropertyName.get(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS);
  }

  public int getSocketTimeoutInSeconds() {
    return (int) valueByPropertyName.get(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS);
  }

  public int getReportTimeoutInSeconds() {
    return (int) valueByPropertyName.get(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS);
  }

  public boolean isNeedsAcknowledgementOfInitialDashboardFilter() {
    return (boolean) valueByPropertyName.get(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER);
  }

  public boolean isEnableDefaultPasswordWarning() {
    return (boolean) valueByPropertyName.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING);
  }

  public Integer getPolicyMonitoringHour() {
    return (Integer) valueByPropertyName.get(SystemConfigurationProperty.POLICY_MONITORING_HOUR);
  }

  public String getDbBackupDir() {
    return (String) valueByPropertyName.get(SystemConfigurationProperty.DB_BACKUP_DIR);
  }

  public String getWebhookSecretPassphrase() {
    return (String) valueByPropertyName.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE);
  }

  public boolean isExternalHyperlinksAllowed() {
    return (boolean) valueByPropertyName.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED);
  }

  public Map<String, String> getMatcherConfiguration() {
    Map<String, String> matcherConfiguration = new HashMap<>();
    for (Entry<String, Object> entry : valueByPropertyName.entrySet()) {
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
}
