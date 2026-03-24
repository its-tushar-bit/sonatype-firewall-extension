/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationInfoTest
    extends AbstractComponentTest
{
  @Inject
  private ConfigurationInfo configurationInfo;

  @Inject
  private Configuration configuration;

  @Test
  public void testGetConfigurationInfo() throws Exception {
    setHdsUrl(null);
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.HDS_URL, "https://clm-staging.sonatype.com/");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.CSRF_PROTECTION, String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.CDN_URL, "http://my-cdn-url/");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES,
        String.valueOf(10));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
        String.valueOf(500));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.USER_AGENT_SUFFIX, "test suffix");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.CSP_ENABLED, String.valueOf(true));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT,
        String.valueOf(20));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD,
        String.valueOf(30));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT,
        String.valueOf(40));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, ",");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS,
        String.valueOf(50));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS,
        String.valueOf(60));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS,
        String.valueOf(70));
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER, String.valueOf(true));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.POLICY_MONITORING_HOUR, String.valueOf(22));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR,
        String.valueOf(12));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.DB_BACKUP_DIR, "sonatype-work");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE,
        "test-passphrase");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS,
        "test-passphrase");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, String.valueOf(true));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.BASE_URL, "http://127.0.0.1:8070");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(true));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST,
        "[\"*first.com\",\"second.*\"]");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.ACCESS_ALLOWLIST,
        "[{\"ipAddress\": \"192.168.33.10\", \"description\": \"Test IPv4 address\"}," +
            "{\"ipAddress\": \"8ed5:9e96:1da1:f53b:587e:9f4d:a7f9:817e\", \"description\": \"Test IPv6 address\"}]");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.PURGE_SCAN_FILES, "newScan");

    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR, "14");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED,
        String.valueOf(false));
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED, "true");
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS, "48");
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, "false");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST,
        "[\"user1\",\"user2\"]");

    configuration.configurationChanged(Sets.newHashSet(
        SystemConfigurationProperty.PURGE_SCAN_FILES,
        SystemConfigurationProperty.ACCESS_ALLOWLIST,
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
        SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS,
        SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED,
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING,
        SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST,
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR,
        SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED,
        SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED,
        SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS,
        SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED,
        SystemConfigurationProperty.API_ACCESS_ALLOW_LIST,
        SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR));

    JsonNode configNode = JsonUtils.parse(configurationInfo.getConfigurationInfo());

    assertThat(configNode.get(SystemConfigurationProperty.HDS_URL).asText()).isEqualTo(
        "https://clm-staging.sonatype.com/");
    assertThat(configNode.get(SystemConfigurationProperty.CSRF_PROTECTION).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.CDN_URL).asText()).isEqualTo("http://my-cdn-url/");
    assertThat(configNode.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES).asText()).isEqualTo("10");
    assertThat(configNode.get(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE).asText()).isEqualTo("500");
    assertThat(configNode.get(SystemConfigurationProperty.USER_AGENT_SUFFIX).asText()).isEqualTo("test suffix");
    assertThat(configNode.get(SystemConfigurationProperty.CSP_ENABLED).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE).asInt()).isEqualTo(1000);
    assertThat(configNode.get(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT).asText()).isEqualTo("20");
    assertThat(configNode.get(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD).asText()).isEqualTo(
        "30");
    assertThat(configNode.get(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT).asText()).isEqualTo("40");
    assertThat(configNode.get(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER).asText()).isEqualTo(
        ",");
    assertThat(configNode.get(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS).asText()).isEqualTo("50");
    assertThat(configNode.get(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS).asText()).isEqualTo("60");
    assertThat(configNode.get(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS).asText()).isEqualTo("70");
    assertThat(configNode.get(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER)
        .asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.POLICY_MONITORING_HOUR).asText()).isEqualTo("22");
    assertThat(configNode.get(SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR).asText())
        .isEqualTo("12");
    assertThat(configNode.get(SystemConfigurationProperty.DB_BACKUP_DIR).asText()).endsWith("sonatype-work");
    assertThat(configNode.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE).asText()).isEqualTo("****");
    assertThat(configNode.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS).asText()).isEqualTo("****");
    assertThat(configNode.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)
        .asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.BASE_URL).asText()).isEqualTo("http://127.0.0.1:8070");
    assertThat(configNode.get(SystemConfigurationProperty.FORCE_BASE_URL).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST).asText())
        .isEqualTo("*first.com,second.*");
    assertThat(configNode.get(SystemConfigurationProperty.ACCESS_ALLOWLIST)).hasToString(
        "[{\"ipAddress\":\"192.168.33.10\",\"description\":\"Test IPv4 address\"}," +
            "{\"ipAddress\":\"8ed5:9e96:1da1:f53b:587e:9f4d:a7f9:817e\",\"description\":\"Test IPv6 address\"}]");
    assertThat(configNode.get(SystemConfigurationProperty.PURGE_SCAN_FILES).asText()).isEqualTo("newScan");
    assertThat(configNode.get(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR).asText()).isEqualTo(
        "14");
    assertThat(configNode.get(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED).asText())
        .isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED).asText()).isEqualTo(
        "true");
    assertThat(configNode.get(SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS)
        .asText()).isEqualTo("48");
    assertThat(configNode.get(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED).asText()).isEqualTo(
        "false");
    assertThat(configNode.get(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST).asText()).isEqualTo(
        "[\"user1\",\"user2\"]");
  }

  @Test
  public void testGetSourceControlConfigurationInfo_noConfig() throws Exception {
    JsonNode configNode = JsonUtils.parse(configurationInfo.getConfigurationInfo());

    assertThat(configNode.get(SystemConfigurationProperty.HDS_URL).asText())
        .isEqualTo("https://clm-staging.sonatype.com/");
    assertThat(configNode.get(SystemConfigurationProperty.CSRF_PROTECTION).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.CDN_URL).asText()).isEqualTo("https://cdn.sonatype.com/");
    assertThat(configNode.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES).asText()).isEqualTo("31457280");
    assertThat(configNode.get(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE).asText()).isEqualTo("500");
    assertThat(configNode.get(SystemConfigurationProperty.USER_AGENT_SUFFIX).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.CSP_ENABLED).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE).asText()).isEqualTo("1000");
    assertThat(configNode.get(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT).asText()).isEqualTo("50");
    assertThat(configNode.get(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD).asText()).isEqualTo(
        "0");
    assertThat(configNode.get(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT).asText()).isEqualTo("2048");
    assertThat(configNode.get(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER).asText()).isEqualTo(
        ",");
    assertThat(configNode.get(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS).asText()).isEqualTo("20");
    assertThat(configNode.get(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS).asText()).isEqualTo("180");
    assertThat(configNode.get(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS).asText()).isEqualTo("2100");
    assertThat(configNode.get(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER)
        .asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.POLICY_MONITORING_HOUR).asText()).isEqualTo("0");
    assertThat(configNode.get(SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR).asText())
        .isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.DB_BACKUP_DIR).asText()).endsWith("db-backup");
    assertThat(configNode.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE).asText()).isEqualTo("****");
    assertThat(configNode.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS).asText()).isEqualTo("****");
    assertThat(configNode.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)
        .asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.BASE_URL).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.FORCE_BASE_URL).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.ACCESS_ALLOWLIST).isEmpty()).isTrue();
    assertThat(configNode.get(SystemConfigurationProperty.PURGE_SCAN_FILES).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR).asText()).isEqualTo(
        "1");
    assertThat(configNode.get(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED).asText())
        .isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED).asText()).isEqualTo(
        "true");
    assertThat(configNode.get(SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS)
        .asText()).isEqualTo("12");
    assertThat(configNode.get(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED)
        .asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST).isEmpty()).isTrue();
    assertThat(configNode.get(SystemConfigurationProperty.MALWARE_DEFENSE_API_MAX_COMPONENTS).asText())
        .isEqualTo("100");
    assertThat(configNode.get(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_COMPONENTS).asText())
        .isEqualTo("1500000");
    assertThat(configNode.get(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_EVENTS).asText())
        .isEqualTo("1500000");
  }

  @Test
  public void testGetConfigurationInfo_PropertiesIncluded() throws IOException {
    // A list of configuration properties that we expect not to be included in the
    // support zip.
    List<String> propertiesExcluded = Arrays.asList(
        "sourceControlEventProcessorPoolSize",
        "sourceControlImportPoolSize",
        "SCHEMA_MIGRATION_ENABLED",
        "sessionTimeout",
        "bfs.artifactoryExpiredTokenRegex",
        "bfs.artifactoryExpiredTokenEmail",
        "bfs.artifactoryAqlBatchSize",
        "bfs.componentQueryLimit",
        "bfs.repositories",
        "quarantinedItemCustomMessage",
        "enterpriseReportingVersionCacheExpirationInMinutes",
        "SAAS_POLICY_MONITOR_POOL_SIZE",
        "skipSbomImportValidation",
        "cleanUpSbomContinuousMonitoringReport",
        "sbomBinaryScanning",
        "sbomContinuousMonitoringUi",
        "sbomPolicies",
        "autoWaivers",
        "idTokenCookieExpirationTime",
        "alpForSbomManager",
        "componentChangeDetectionApi",
        "containerImagesEvalEnabled",
        "zScaler",
        "thirdPartyKevLookup",
        "userManagementPages",
        "epssDataEnabled",
        "integrationsSupportedVersionCount",
        "userActivityTracking",
        "copyStorageConfig",
        "warnOnNonPrimaryStorageAccess",
        "waiverRequestWorkflowEnabled",
        "userTokenDefaultExpirationDays",
        "exitOnFatalError",
        "maliciousUrlsPartnerAccess",
        "maxConcurrentTenantIndexCreation",
        "evaluationQueueConfig");

    // Properties included in the config.json in support zip
    JsonNode configNode = JsonUtils.parse(configurationInfo.getConfigurationInfo());

    for (ConfigurationProperty property : ConfigurationProperty.PROPERTIES) {
      String name = property.getName();
      if (configNode.get(name) == null) {
        if (!propertiesExcluded.contains(name)) {
          throw new RuntimeException("config.json in support zip is missing expected property: " + name);
        }
      }
    }
  }
}
