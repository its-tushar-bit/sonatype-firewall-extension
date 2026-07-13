/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import static com.sonatype.insight.brain.support.SystemInfo.MASK;

import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.json.store.JsonUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @since 1.143
 */
@Named
@Singleton
public class ConfigurationInfo
{
  private final Configuration configuration;

  @Inject
  ConfigurationInfo(Configuration configuration) {
    this.configuration = configuration;
  }

  String getConfigurationInfo() {
    final SortedMap<String, Object> entries = new TreeMap<>();
    entries.put(SystemConfigurationProperty.HDS_URL, configuration.getHdsUrl());
    entries.put(SystemConfigurationProperty.RELAY_URL, configuration.getRelayUrl());
    entries.put(SystemConfigurationProperty.PURGE_SCAN_FILES, configuration.getPurgeScanFiles());
    entries.put(SystemConfigurationProperty.CSRF_PROTECTION, configuration.isAntiCsrfEnabled());
    entries.put(SystemConfigurationProperty.CDN_URL, configuration.getCdnUrl());
    entries.put(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, configuration.getSupportReadLimitBytes());
    entries.put(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX,
        configuration.getSupportClusterLogFileRegex());
    entries.put(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
        configuration.getEventBusMaxThreadPoolSize());
    entries.put(SystemConfigurationProperty.USER_AGENT_SUFFIX, configuration.getUserAgentSuffix());
    entries.put(SystemConfigurationProperty.CSP_ENABLED, configuration.isCspEnabled());
    entries.put(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, configuration.isBlockSemicolon());
    entries.put(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, configuration.isBlockBackslash());
    entries.put(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, configuration.isBlockNonAscii());
    entries.put(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE, configuration.getReleaseGraphCacheSize());
    entries.put(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT,
        configuration.getLicenseLegalHdsRequestLimit());
    entries.put(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD,
        configuration.getMaxApplicationsToQueryOnDashboard());
    entries.put(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT,
        configuration.getMaxAdvancedSearchClauseCount());
    entries.put(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER,
        configuration.getAdvancedSearchCSVExportDelimiter());
    entries.put(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS, configuration.getConnectTimeoutInSeconds());
    entries.put(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS, configuration.getSocketTimeoutInSeconds());
    entries.put(SystemConfigurationProperty.FIREWALL_QUARANTINE_HDS_CONNECT_TIMEOUT_IN_SECONDS,
        configuration.getFirewallQuarantineHdsConnectTimeoutInSeconds());
    entries.put(SystemConfigurationProperty.FIREWALL_QUARANTINE_HDS_SOCKET_TIMEOUT_IN_SECONDS,
        configuration.getFirewallQuarantineHdsSocketTimeoutInSeconds());
    entries.put(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS, configuration.getReportTimeoutInSeconds());
    entries.put(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
        configuration.isNeedsAcknowledgementOfInitialDashboardFilter());
    entries.put(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
        configuration.isEnableDefaultPasswordWarning());
    entries.put(SystemConfigurationProperty.POLICY_MONITORING_HOUR, configuration.getPolicyMonitoringHour());
    entries.put(SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR,
        configuration.getHistoricalPolicyViolationTelemetryHour());
    entries.put(SystemConfigurationProperty.CONTINUOUS_MONITORING_WORKER_THREADS,
        configuration.getContinuousMonitoringWorkerThreads());
    entries.put(SystemConfigurationProperty.MAX_CONTINUOUS_MONITORING_RETRIES,
        configuration.getMaxContinuousMonitoringRetries());
    entries.put(SystemConfigurationProperty.CONTINUOUS_MONITORING_JITTER_MINUTES,
        configuration.getContinuousMonitoringJitterMinutes());
    entries.put(SystemConfigurationProperty.CONTINUOUS_MONITORING_POLL_INTERVAL_MS,
        configuration.getContinuousMonitoringPollIntervalMs());
    entries.put(SystemConfigurationProperty.CONTINUOUS_MONITORING_TICK_BATCH_SIZE,
        configuration.getContinuousMonitoringTickBatchSize());
    entries.put(SystemConfigurationProperty.CONTINUOUS_MONITORING_IDLE_BACKOFF_MS,
        configuration.getContinuousMonitoringIdleBackoffMs());
    entries.put(SystemConfigurationProperty.DB_BACKUP_DIR, configuration.getDbBackupDir());
    entries.put(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE, MASK);
    entries.put(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS, MASK);
    entries.put(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED, configuration.isExternalHyperlinksAllowed());
    entries.put(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING,
        configuration.getMatcherConfiguration() != null
            ? configuration.getMatcherConfiguration()
                .get(
                    "disableConanNamespaceMatching")
            : null);
    entries.put(SystemConfigurationProperty.BASE_URL,
        configuration.getBaseUrlConfiguration() != null ? configuration.getBaseUrlConfiguration().getBaseUrl() : null);
    entries.put(SystemConfigurationProperty.FORCE_BASE_URL,
        configuration.getBaseUrlConfiguration() != null
            ? configuration.getBaseUrlConfiguration()
                .isForceBaseUrl()
            : null);
    entries.put(SystemConfigurationProperty.ACCESS_ALLOWLIST, configuration.getAccessAllowlist());
    entries.put(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED,
        configuration.isALPObservedLicenseDetectionEnabled());
    entries.put(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR,
        configuration.getWaivedComponentUpgradeInspectionHour());
    entries.put(SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS,
        configuration.getQuarantinedComponentReportExpirationTimeInHours());
    entries.put(SystemConfigurationProperty.HOSTED_DEPLOYMENT_BLOCK_RETENTION_HOURS,
        configuration.getHostedDeploymentBlockRetentionHours());
    entries.put(SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID, configuration.getSuccessMetricsStageId());

    List<String> frameAncestorsAllowList = configuration.getFrameAncestorsAllowList();
    if (frameAncestorsAllowList != null) {
      entries.put(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST, String.join(",", frameAncestorsAllowList));
    }
    else {
      entries.put(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST, null);
    }
    entries.put(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES,
        configuration.getAutomaticQuarantineReleaseTimeIntervalInMinutes());
    entries.put(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED,
        configuration.getWaivedComponentUpgradeMonitoringEnabled());
    entries.put(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED,
        configuration.getAdvanceReportingInsightsEnabled());
    entries.put(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST,
        ConfigurationUtils.listToStringDuplicatesRemoved(configuration.getApiAccessAllowList()));
    entries.put(SystemConfigurationProperty.MALWARE_DEFENSE_API_MAX_COMPONENTS,
        configuration.getMalwareDefenseApiMaxComponents());
    entries.put(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_COMPONENTS,
        configuration.getComponentChangeDetectionMaxComponents());
    entries.put(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_BATCH_SIZE,
        configuration.getComponentChangeDetectionBatchSize());
    entries.put(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_TASK_PERIOD,
        configuration.getComponentChangeDetectionTaskPeriod());
    entries.put(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_EVENTS,
        configuration.getComponentChangeDetectionMaxEvents());
    entries.put(SystemConfigurationProperty.ZSCALER_UPDATE_TASK_PERIOD,
        configuration.getZScalerUpdateTaskPeriod());
    entries.put(SystemConfigurationProperty.ZSCALER_MAX_URLS_PER_CATEGORY,
        configuration.getZScalerMaxUrlsPerCategory());

    return JsonUtils.format(entries);
  }
}
