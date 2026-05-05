/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.git.SourceControlImportThreadPoolExecutor;
import com.sonatype.insight.brain.git.event.orchestrate.SourceControlEventProcessor;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConfig;
import com.sonatype.insight.brain.repository.hosted.HostedComponentScanQueueConfig;
import com.sonatype.insight.brain.security.FIPSConfig;
import com.sonatype.insight.brain.service.CopyStorageConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Triple;

import static com.sonatype.insight.brain.policy.evaluator.PolicyMonitor.POLICY_MONITOR_THREADS_DEFAULT;

public class ConfigurationProperty
{
  // Visible for testing
  public static final ConfigurationProperty[] PROPERTIES = new ConfigurationProperty[]{
    new ConfigurationProperty(SystemConfigurationProperty.BASE_URL, String.class,
        (p, s) -> s,
        (p, o) -> ConfigurationUtils.urlValueToString(o)),
    new ConfigurationProperty(SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID, String.class,
        (p, s) -> s,
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.FORCE_BASE_URL, Boolean.class,
        (p, s) -> Boolean.parseBoolean(s),
        (p, o) -> ConfigurationUtils.forceBaseUrlToString(
            ConfigurationUtils.getParameter(p, TransactionContext.class), o)),
    new ConfigurationProperty(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST, List.class,
        (p, s) -> ConfigurationUtils.stringToFrameAncestorsAllowlist(s),
        (p, o) -> ConfigurationUtils.frameAncestorsAllowlistToString((List<String>) o)),
    new ConfigurationProperty(SystemConfigurationProperty.HDS_URL, String.class,
        (p, s) -> ConfigurationUtils.hdsUrl(ConfigurationUtils.getParameter(p, InsightConfig.class), s),
        (p, o) -> ConfigurationUtils.urlValueToString(o)),
    new ConfigurationProperty(SystemConfigurationProperty.CDN_URL, String.class,
        (p, s) -> Objects.toString(s, "https://cdn.sonatype.com/"),
        (p, o) -> ConfigurationUtils.urlValueToString(o)),
    new ConfigurationProperty(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES, Long.class,
        (p, s) -> NumberUtils.toLong(s, 31457280),
        (tx, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.SUPPORT_CLUSTER_LOG_FILE_REGEX, String.class,
        (p, s) -> ConfigurationUtils.clusterLogFileRegex(s),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.ACCESS_ALLOWLIST, List.class,
        (p, s) -> ConfigurationUtils.stringToAccessAllowlist(s),
        (p, o) -> ConfigurationUtils.accessAllowlistToString((List<Map<String, String>>) o)),
    new ConfigurationProperty(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE, Integer.class,
        (p, s) -> ConfigurationUtils.getEventBusThreadPoolSize(s, AsyncEventBus.DEFAULT_MAX_POOL_SIZE),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE, Integer.class,
        (p, s) -> ConfigurationUtils.getSourceControlEventProcessorPoolSize(s,
            SourceControlEventProcessor.DEFAULT_MAX_THREAD_POOL_SIZE),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.SOURCE_CONTROL_IMPORT_POOL_SIZE, Integer.class,
        (p, s) -> ConfigurationUtils.getSourceControlImportPoolSize(s,
            SourceControlImportThreadPoolExecutor.DEFAULT_MAX_THREAD_POOL_SIZE),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.CSRF_PROTECTION, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.USER_AGENT_SUFFIX, String.class,
        (p, s) -> s,
        (p, o) -> ConfigurationUtils.userAgentSuffix(o)),
    new ConfigurationProperty(SystemConfigurationProperty.CSP_ENABLED, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, Boolean.class,
        (p, s) -> Boolean.parseBoolean(s),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 1000),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 50),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD, Integer.class,
        (p, s) -> NumberUtils.toInt(s),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 2048),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.MAX_CONCURRENT_TENANT_INDEX_CREATION, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 5),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, String.class,
        (p, s) -> Objects.toString(s, ","),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 20),
        (p, o) -> ConfigurationUtils.integerValueToString(o, SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS,
            5, 60 * 60)),
    new ConfigurationProperty(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 3 * 60),
        (p, o) -> ConfigurationUtils.integerValueToString(o, SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS, 5,
            60 * 60)),
    new ConfigurationProperty(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 35 * 60),
        (p, o) -> ConfigurationUtils.integerValueToString(o, SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS,
            30, 60 * 300)),
    new ConfigurationProperty(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
        Boolean.class,
        (p, s) -> Boolean.parseBoolean(s),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.POLICY_MONITORING_HOUR, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 0),
        (p, o) -> ConfigurationUtils.integerValueToString(o, SystemConfigurationProperty.POLICY_MONITORING_HOUR,
            0, 23)),
    new ConfigurationProperty(SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR, Integer.class,
        (p, s) -> s == null ? null : NumberUtils.toInt(s, 0), (p, o) -> ConfigurationUtils.integerValueToString(o,
            SystemConfigurationProperty.HISTORICAL_POLICY_VIOLATION_TELEMETRY_HOUR, 0, 23)),
    new ConfigurationProperty(SystemConfigurationProperty.DB_BACKUP_DIR, String.class,
        (p, s) -> ConfigurationUtils.absolutePathOrRelativeToSonatypeWork(
            ConfigurationUtils.getParameter(p, InsightConfig.class),
            Objects.toString(s, InsightConfig.DEFAULT_BACKUP_DIR)),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE, String.class,
        (p, s) -> Objects.toString(s, "^d1swM!FF&qQ"),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE_FIPS, String.class,
        (p, s) -> Objects.toString(s, FIPSConfig.getFipsWebhookSecretPassphraseOrDefault()),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING,
        Boolean.class,
        (p, s) -> Boolean.parseBoolean(s),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.SCHEMA_MIGRATION_ENABLED,
        Boolean.class,
        (p, s) -> ConfigurationUtils.schemaMigrationEnabled(s),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.SESSION_TIMEOUT_MINUTES, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 30),
        (p, o) -> ConfigurationUtils.sessionTimeoutToString(o)),
    new ConfigurationProperty(SystemConfigurationProperty.QUARANTINED_COMPONENT_REPORT_EXPIRATION_TIME_IN_HOURS,
        Integer.class, (p, s) -> NumberUtils.toInt(s, 12), (p, o) -> Objects.toString(o, "12")),
    new ConfigurationProperty(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_REGEX, String.class,
        (p, s) -> Objects.toString(s, "(?i)(?s).*token[\\s\\w:]+expired.*"),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL, String.class,
        (p, s) -> s,
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.BFS_ARTIFACTORY_AQL_BATCH_SIZE, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 1000),
        (p, o) -> ConfigurationUtils.integerValueToString(o,
            SystemConfigurationProperty.BFS_ARTIFACTORY_AQL_BATCH_SIZE, 0, 1000)),
    new ConfigurationProperty(SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, Integer.class,
        (p, s) -> NumberUtils.createInteger(s),
        (p, o) -> ConfigurationUtils.integerValueToString(o, SystemConfigurationProperty.BFS_COMPONENT_QUERY_LIMIT, 0,
            Integer.MAX_VALUE)),
    new ConfigurationProperty(SystemConfigurationProperty.BFS_REPOSITORIES, String.class,
        (p, s) -> s,
        (p, o) -> ConfigurationUtils.parseRepositoryList((String) o)),
    new ConfigurationProperty(SystemConfigurationProperty.PURGE_SCAN_FILES, String.class,
        (p, s) -> s,
        (p, o) -> (String) ConfigurationUtils.purgeScanFiles((String) o)),
    new ConfigurationProperty(SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES,
        Integer.class,
        (p, s) -> NumberUtils.toInt(s, 60),
        (p, o) -> ConfigurationUtils.integerValueToString(o,
            SystemConfigurationProperty.AUTOMATIC_QUARANTINE_RELEASE_TIME_INTERVAL_IN_MINUTES,
            30, Integer.MAX_VALUE)),
    new ConfigurationProperty(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR,
        Integer.class,
        (p, s) -> NumberUtils.createInteger(s),
        (p, o) -> ConfigurationUtils.integerValueToString(o,
            SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_INSPECTION_HOUR, 0, 23)),
    new ConfigurationProperty(SystemConfigurationProperty.WAIVED_COMPONENT_UPGRADE_MONITORING_ENABLED, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, false),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.ALP_OBSERVED_LICENSE_DETECTION_ENABLED,
        Boolean.class,
        (p, s) -> Boolean.parseBoolean(s),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE, String.class,
        (p, s) -> s,
        (p, o) -> ConfigurationUtils.validateCustomMessage(o)),
    new ConfigurationProperty(SystemConfigurationProperty.ENTERPRISE_REPORTING_VERSION_CACHE_EXPIRATION_IN_MINUTES,
        Integer.class,
        (p, s) -> NumberUtils.toInt(s, 10),
        (p, o) -> ConfigurationUtils.integerValueToString(o,
            SystemConfigurationProperty.ENTERPRISE_REPORTING_VERSION_CACHE_EXPIRATION_IN_MINUTES, 1, 120)),
    new ConfigurationProperty(SystemConfigurationProperty.SAAS_POLICY_MONITOR_POOL_SIZE, Integer.class,
        (p, s) -> ConfigurationUtils.getSaasPolicyMonitorPoolSize(s, POLICY_MONITOR_THREADS_DEFAULT),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST, List.class,
        (p, s) -> ConfigurationUtils.stringToList(s),
        (p, o) -> ConfigurationUtils.listToStringDuplicatesRemoved((List<String>) o)),
    new ConfigurationProperty(SystemConfigurationProperty.SKIP_SBOM_IMPORT_VALIDATION, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, false),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.SBOM_BINARY_SCANNING, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.SBOM_CONTINUOUS_MONITORING_UI, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.SBOM_POLICIES, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, false),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.AUTO_WAIVERS, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.MALWARE_DEFENSE_API_MAX_COMPONENTS, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 100),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_COMPONENTS, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 1_500_000),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_BATCH_SIZE, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 100),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_TASK_PERIOD, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 24),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_EVENTS, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 1_500_000),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.ALP_FOR_SBOM_MANAGER, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, false),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_API, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, false),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.CONTAINER_IMAGES_EVAL_ENABLED, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.ZSCALER_UPDATE_TASK_PERIOD, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 24),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.ZSCALER_MAX_URLS_PER_CATEGORY, Integer.class,
        (p, s) -> NumberUtils.toInt(s, 25000),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.ZSCALER, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.THIRD_PARTY_KEV_LOOKUP, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.USER_MANAGEMENT_PAGES, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.EPSS_DATA, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, false),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.INTEGRATIONS_SUPPORTED_VERSION_COUNT, Integer.class,
        (p, s) -> NumberUtils.createInteger(s),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, false),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.USER_ACTIVITY_TRACKING, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, false),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.EXIT_ON_FATAL_ERROR, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, true),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.COPY_STORAGE_CONFIG, Map.class,
        (p, s) -> ConfigurationUtils.stringToObject(
            s,
            CopyStorageConfig.class,
            new CopyStorageConfig(1, 1)),
        (p, o) -> {
          CopyStorageConfig copyStorageConfig = JsonUtils.convertValue(o, CopyStorageConfig.class);
          if (copyStorageConfig == null) {
            return null;
          }
          copyStorageConfig.validate();
          return ConfigurationUtils.objectToString(copyStorageConfig);
        }),
    new ConfigurationProperty(SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS, Integer.class,
        (p, s) -> s == null ? null : NumberUtils.toInt(s),
        (p, o) -> ConfigurationUtils.integerValueToString(o,
            SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS, 1, 365)),
    new ConfigurationProperty(SystemConfigurationProperty.MALICIOUS_URLS_PARTNER_ACCESS, Boolean.class,
        (p, s) -> ConfigurationUtils.parseBooleanWithDefault(s, false),
        (p, o) -> Objects.toString(o, null)),
    new ConfigurationProperty(SystemConfigurationProperty.EVALUATION_QUEUE_CONFIG, Map.class,
        (p, s) -> ConfigurationUtils.stringToObject(
            s,
            EvaluationQueueConfig.class,
            EvaluationQueueConfig.builder().build()),
        (p, o) -> {
          if (o == null) {
            return null;
          }
          @SuppressWarnings("unchecked")
          Map<String, Object> overrides = JsonUtils.convertValue(o, Map.class);
          String existingValue = ConfigurationUtils.getParameter(p, String.class);
          EvaluationQueueConfig base = ConfigurationUtils.stringToObject(
              existingValue,
              EvaluationQueueConfig.class,
              EvaluationQueueConfig.builder().build());
          EvaluationQueueConfig merged = EvaluationQueueConfig.merge(base, overrides);
          return ConfigurationUtils.objectToString(merged);
        }),
    new ConfigurationProperty(SystemConfigurationProperty.HOSTED_SCAN_QUEUE_CONFIG, Map.class,
        (p, s) -> ConfigurationUtils.stringToObject(
            s,
            HostedComponentScanQueueConfig.class,
            HostedComponentScanQueueConfig.defaultConfig()),
        (p, o) -> {
          if (o == null) {
            return null;
          }
          @SuppressWarnings("unchecked")
          Map<String, Object> overrides = JsonUtils.convertValue(o, Map.class);
          String existingValue = ConfigurationUtils.getParameter(p, String.class);
          HostedComponentScanQueueConfig base = ConfigurationUtils.stringToObject(
              existingValue,
              HostedComponentScanQueueConfig.class,
              HostedComponentScanQueueConfig.defaultConfig());
          HostedComponentScanQueueConfig merged = HostedComponentScanQueueConfig.merge(base, overrides);
          return ConfigurationUtils.objectToString(merged);
        }),
    new ConfigurationProperty(SystemConfigurationProperty.LIFECYCLE_TIER, String.class,
        (p, s) -> s,
        (p, o) -> Objects.toString(o, null)),
  };

  protected static final Map<String, ConfigurationProperty> PROPERTY_BY_NAME = Arrays.stream(PROPERTIES)
      .collect(
          Collectors.toMap(ConfigurationProperty::getName, Function.identity()));

  private static final Map<String, ConfigurationProperty> BOOLEAN_PROPERTIES_BY_NAME = Collections.unmodifiableMap(
      Arrays.stream(PROPERTIES)
          .filter(property -> property.getType().equals(Boolean.class))
          .collect(Collectors.toMap(ConfigurationProperty::getName, Function.identity())));

  // Properties that any authenticated user can read (no specific permissions required beyond authentication)
  public static final Set<String> PUBLIC_PROPERTIES = Collections.singleton(
      SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS);

  // Multiple permissions grouped by each Triple will be ORed in order to check access
  public static final Map<String, Set<Triple<OwnerType, String, Set<Permission>>>> additionalPermissionsPerProperty =
      ImmutableMap.of(
          SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE,
          Sets.newHashSet(Triple.of(OwnerType.ORGANIZATION,
              Organization.ROOT_ORGANIZATION_ID,
              Sets.newHashSet(Permission.EVALUATE_COMPONENT))));

  private final String name;

  private final Class<?> type;

  private final BiFunction<Object[], String, Object> stringToValue;

  private final BiFunction<Object[], Object, String> valueToString;

  public ConfigurationProperty(
      final String name,
      final Class<?> type,
      final BiFunction<Object[], String, Object> stringToValue,
      final BiFunction<Object[], Object, String> valueToString)
  {
    this.name = name;
    this.type = type;
    this.stringToValue = stringToValue;
    this.valueToString = valueToString;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }

  public BiFunction<Object[], String, Object> getStringToValue() {
    return stringToValue;
  }

  public BiFunction<Object[], Object, String> getValueToString() {
    return valueToString;
  }

  public static Map<String, ConfigurationProperty> getConfigurationPropertiesByName() {
    return PROPERTY_BY_NAME;
  }

  public static Map<String, ConfigurationProperty> getBooleanConfigurationPropertiesByName() {
    return BOOLEAN_PROPERTIES_BY_NAME;
  }
}
