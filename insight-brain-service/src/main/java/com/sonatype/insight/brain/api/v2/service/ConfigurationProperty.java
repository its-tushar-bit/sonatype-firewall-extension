/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
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
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Triple;

public class ConfigurationProperty
{
  protected static final ConfigurationProperty[] PROPERTIES = new ConfigurationProperty[]{
      new ConfigurationProperty(SystemConfigurationProperty.BASE_URL, String.class,
          (p, s) -> s,
          (p, o) -> ConfigurationUtils.urlValueToString(o)),
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
          (p, s) -> StringUtils.defaultString(s, "https://cdn.sonatype.com/"),
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
      new ConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, String.class,
          (p, s) -> StringUtils.defaultString(s, ","),
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
      new ConfigurationProperty(SystemConfigurationProperty.DB_BACKUP_DIR, String.class,
          (p, s) -> ConfigurationUtils.absolutePathOrRelativeToSonatypeWork(
              ConfigurationUtils.getParameter(p, InsightConfig.class),
              StringUtils.defaultString(s, InsightConfig.DEFAULT_BACKUP_DIR)),
          (p, o) -> Objects.toString(o, null)),
      new ConfigurationProperty(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE, String.class,
          (p, s) -> StringUtils.defaultString(s, "^d1swM!FF&qQ"),
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
          (p, s) -> StringUtils.defaultString(s, "(?i)(?s).*token[\\s\\w:]+expired.*"),
          (p, o) -> Objects.toString(o, null)),
      new ConfigurationProperty(SystemConfigurationProperty.BFS_ARTIFACTORY_EXPIRED_TOKEN_EMAIL, String.class,
          (p, s) -> s,
          (p, o) -> Objects.toString(o, null)),
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
      };

  protected static final Map<String, ConfigurationProperty> PROPERTY_BY_NAME = Arrays.stream(PROPERTIES).collect(
      Collectors.toMap(ConfigurationProperty::getName, Function.identity()));

  // Multiple permissions grouped by each Triple will be ORed in order to check access
  public static final Map<String, Set<Triple<OwnerType, String, Set<Permission>>>> additionalPermissionsPerProperty =
      ImmutableMap.of(
          SystemConfigurationProperty.QUARANTINED_ITEM_CUSTOM_MESSAGE,
          Sets.newHashSet(Triple.of(OwnerType.ORGANIZATION,
              Organization.ROOT_ORGANIZATION_ID,
              Sets.newHashSet(Permission.EVALUATE_COMPONENT)))
      );

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
}
