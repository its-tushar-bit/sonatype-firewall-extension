/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrations;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.AllowedIp;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationUtils
{
  private static final Logger log = LoggerFactory.getLogger(ConfigurationUtils.class);

  public static final int MAX_USER_AGENT_SUFFIX_SIZE = 128;

  public static final String LONG_USER_AGENT_SUFFIX_ERROR_MSG = "The user agent suffix cannot exceed %s characters.";

  public static final String INVALID_USER_AGENT_SUFFIX_ERROR_MSG = "The user agent suffix is invalid.";

  public static final String OUTSIDE_RANGE_ERROR_MSG =
      "The %s must be greater than or equal to %s and less than or equal to %s.";

  public static final String INVALID_CHARACTERS_ERROR_MSG = "The repository list must be comma-separated " +
      "and each repository can only contain alphanumeric characters, underscores, and hyphens. Invalid repository: %s";

  public static final String INVALID_PURGE_SCAN_FILES_VALUE_MSG = "Provided value: [%s] " +
      "for property purgeScanFiles is invalid";

  public static final int MAX_QUARANTINED_ITEM_CUSTOM_MESSAGE_SIZE = 500;

  public static final String LONG_QUARANTINED_ITEM_CUSTOM_MESSAGE_ERROR_MSG =
      "The quarantined item custom message cannot exceed %s characters.";

  public static final String NONE_VALUE = "'none'";

  public static final String WITH_REPORTS = "withReports";

  public static final String NXIQ_EVENT_BUS_MAX_THREAD_POOL_SIZE = "NXIQ_EVENT_BUS_MAX_THREAD_POOL_SIZE";

  public static final String NXIQ_SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE =
      "NXIQ_SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE";

  public static final String NXIQ_SOURCE_CONTROL_IMPORT_POOL_SIZE = "NXIQ_SOURCE_CONTROL_IMPORT_POOL_SIZE";

  public static final String NXIQ_SAAS_POLICY_MONITOR_POOL_SIZE = "NXIQ_SAAS_POLICY_MONITOR_POOL_SIZE";

  private static SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private ConfigurationUtils() {
  }

  @Inject
  public static void injectDependencies(final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO) {
    ConfigurationUtils.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  public static String urlValueToString(Object value) {
    if (value == null) {
      return null;
    }
    String url = addEndingForwardSlashIfNeeded(value.toString());
    validateUrl(url);
    return url;
  }

  private static String addEndingForwardSlashIfNeeded(String url) {
    if (url != null && !url.endsWith("/")) {
      url += '/';
    }
    return url;
  }

  private static void validateUrl(String url) {
    try {
      new URL(url);
    }
    catch (Exception e) {
      throw new BadRequestException("Invalid URL: " + url, e);
    }
  }

  public static String forceBaseUrlToString(TransactionContext tx, Object value) {
    Boolean booleanValue = (Boolean) value;
    String baseUrl = systemConfigurationPropertyDAO.get(tx, SystemConfigurationProperty.BASE_URL);
    if (booleanValue != null && booleanValue) {
      log.error("DEPRECATION NOTICE: Forcing use of server base URL: {}, any 'X-Forwarded-*' headers will be " +
          "ignored. More information at http://links.sonatype.com/products/clm/docs/base-url", baseUrl);
    }
    else {
      log.info("Server base URL: {}", baseUrl);
    }
    return booleanValue == null ? null : Boolean.toString(booleanValue);
  }

  public static List<String> stringToFrameAncestorsAllowlist(String value) {
    if (value != null) {
      try {
        return JsonUtils.parse(value.getBytes(), List.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException("Invalid json: " + value, e);
      }
    }
    return null;
  }

  public static String frameAncestorsAllowlistToString(List<String> values) {
    if (values != null) {

      if (values.contains(NONE_VALUE)) {
        return JsonUtils.writeUnformatted(Collections.singletonList(NONE_VALUE));
      }
      List<String> nullFiltered =
          values.stream()
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      if (!nullFiltered.isEmpty()) {
        List<String> result =
            Stream.concat(Stream.of("'self'"), nullFiltered.stream())
                .distinct()
                .collect(Collectors.toList());
        return JsonUtils.writeUnformatted(result);
      }
    }
    return null;
  }

  public static List<AllowedIp> stringToAccessAllowlist(String values) {
    if (values != null) {
      try {
        return JsonUtils.parse(values, new TypeReference<List<AllowedIp>>()
        {
        });
      }
      catch (IOException e) {
        throw new UncheckedIOException("Invalid json: " + values, e);
      }
    }
    return null;
  }

  public static String accessAllowlistToString(List<Map<String, String>> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }

    List<String> invalids = new ArrayList<>();
    for (Map<String, String> value : values) {
      if (value == null) {
        invalids.add("null");
        continue;
      }
      String ipAddress = null;
      try {
        ipAddress = value.get("ipAddress");
      }
      catch (ClassCastException e) {
        invalids.add("Invalid type, expected String");
        continue;
      }
      if (isInvalidAllowlistIP(ipAddress)) {
        invalids.add(ipAddress);
      }
    }
    if (!invalids.isEmpty()) {
      throw new BadRequestException(String.format("Invalid IP addresses: %s", invalids));
    }

    return JsonUtils.writeUnformatted(values);
  }

  private static boolean isInvalidAllowlistIP(String allowlistIp) {
    if (Strings.isNullOrEmpty(allowlistIp)) {
      return true;
    }
    IPAddress addr = new IPAddressString(allowlistIp).getAddress();
    if (addr == null) {
      return true;
    }
    return addr.getPrefixLength() != null &&
        !addr.mask(addr.getNetworkMask()).toInetAddress().equals(addr.toInetAddress());
  }

  public static List<String> stringToList(String values) {
    if (values != null) {
      try {
        return JsonUtils.parse(values, List.class);
      }
      catch (IOException e) {
        throw new UncheckedIOException("Invalid json: " + values, e);
      }
    }
    return null;
  }

  /**
   * Converts a List to its String representation (JSON), by also removing any duplicated items.
   * Empty lists return `null`.
   */
  public static String listToStringDuplicatesRemoved(List<String> values) {
    if (values != null && !values.isEmpty()) {
      Set<String> noDuplicates = new HashSet<>(values);
      return JsonUtils.writeUnformatted(noDuplicates);
    }
    return null;
  }

  public static String userAgentSuffix(Object userAgentSuffix) {
    String userAgentSuffixValue = (String) userAgentSuffix;
    if (userAgentSuffix == null) {
      return userAgentSuffixValue;
    }
    if (userAgentSuffixValue.length() > MAX_USER_AGENT_SUFFIX_SIZE) {
      throw new BadRequestException(String.format(LONG_USER_AGENT_SUFFIX_ERROR_MSG, MAX_USER_AGENT_SUFFIX_SIZE));
    }
    if (!userAgentSuffixValue.matches("[^\\p{Cntrl}]*")) {
      throw new BadRequestException(INVALID_USER_AGENT_SUFFIX_ERROR_MSG);
    }
    return userAgentSuffixValue;
  }

  public static Boolean parseBooleanWithDefault(String booleanString, Boolean defaultValue) {
    if (booleanString == null) {
      return defaultValue;
    }
    return Boolean.valueOf(booleanString);
  }

  public static String hdsUrl(InsightConfig insightConfig, String hdsUrl) {
    if (hdsUrl != null) {
      return hdsUrl;
    }
    if (insightConfig.getHdsUrl() != null) {
      return insightConfig.getHdsUrl();
    }
    return "https://clm.sonatype.com/";
  }

  @SuppressWarnings("unchecked")
  public static <T> T getParameter(Object[] parameters, Class<T> clazz) {
    for (Object p : parameters) {
      if (ClassUtils.isAssignable(p.getClass(), clazz)) {
        return (T) p;
      }
    }
    return null;
  }

  public static String integerValueToString(Object value, String name, int min, int max) {
    Integer v = (Integer) value;
    if (v == null) {
      return null;
    }
    if (v < min || v > max) {
      throw new BadRequestException(String.format(OUTSIDE_RANGE_ERROR_MSG, name, min, max));
    }
    return Integer.toString(v);
  }

  public static String absolutePathOrRelativeToSonatypeWork(InsightConfig insightConfig, String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      file = new File(insightConfig.getSonatypeWork(), path);
    }
    return file.getAbsolutePath();
  }

  public static boolean schemaMigrationEnabled(String schemaMigrationEnabledFromDatabase) {
    Boolean migrationEnabled = DatabaseMigrations.isMigrationEnabledFromEnvironmentVariable();
    if (migrationEnabled != null) {
      return migrationEnabled;
    }
    return parseBooleanWithDefault(schemaMigrationEnabledFromDatabase, true);
  }

  public static String clusterLogFileRegex(String clusterLogFileRegexFromDatabase) {
    String clusterLogFileRegexFromEnvironmentVariable =
        System.getenv(InsightConfig.NXIQ_SUPPORT_CLUSTER_LOG_FILE_REGEX);
    if (clusterLogFileRegexFromEnvironmentVariable != null) {
      return clusterLogFileRegexFromEnvironmentVariable;
    }
    if (clusterLogFileRegexFromDatabase != null) {
      return clusterLogFileRegexFromDatabase;
    }
    return InsightConfig.DEFAULT_SUPPORT_CLUSTER_LOG_FILE_REGEX;
  }

  public static String sessionTimeoutToString(Object timeout) {
    int minutes = (int) timeout;
    if (minutes < 3 || minutes > 120) {
      throw new RuntimeException("Timeout configuration should be in range from 3 to 120 minutes.");
    }
    return Objects.toString(minutes, null);
  }

  public static String parseRepositoryList(String repositoryList) {
    if (repositoryList == null) {
      return null;
    }
    Set<String> repositories = new LinkedHashSet<>();
    for (String repository : repositoryList.split(",")) {
      String trimmedRepository = repository.trim();
      if (trimmedRepository.matches("^[\\w\\-]+$")) {
        repositories.add(trimmedRepository);
      }
      else {
        throw new BadRequestException(String.format(INVALID_CHARACTERS_ERROR_MSG, repository));
      }
    }
    return String.join(",", repositories);
  }

  public static Object purgeScanFiles(String purgeScan) {
    if (WITH_REPORTS.equals(purgeScan)) {
      return purgeScan;
    }
    else {
      throw new BadRequestException(String.format(INVALID_PURGE_SCAN_FILES_VALUE_MSG, purgeScan));
    }
  }

  public static String validateCustomMessage(Object customMessage) {
    if (customMessage == null) {
      return null;
    }
    String customMessageValue = customMessage.toString();
    if (customMessageValue.length() > MAX_QUARANTINED_ITEM_CUSTOM_MESSAGE_SIZE) {
      throw new BadRequestException(
          String.format(LONG_QUARANTINED_ITEM_CUSTOM_MESSAGE_ERROR_MSG, MAX_QUARANTINED_ITEM_CUSTOM_MESSAGE_SIZE));
    }
    return customMessageValue;
  }

  public static int getEventBusThreadPoolSize(String value, int defaultVal) {
    if (Strings.isNullOrEmpty(value)) {
      return getIntEnvValueOrDefault(NXIQ_EVENT_BUS_MAX_THREAD_POOL_SIZE, defaultVal);
    }
    return NumberUtils.toInt(value, defaultVal);
  }

  public static int getSourceControlEventProcessorPoolSize(String value, int defaultVal) {
    if (Strings.isNullOrEmpty(value)) {
      return getIntEnvValueOrDefault(NXIQ_SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE, defaultVal);
    }
    return NumberUtils.toInt(value, defaultVal);
  }

  public static int getSourceControlImportPoolSize(String value, int defaultVal) {
    if (Strings.isNullOrEmpty(value)) {
      return getIntEnvValueOrDefault(NXIQ_SOURCE_CONTROL_IMPORT_POOL_SIZE, defaultVal);
    }
    return NumberUtils.toInt(value, defaultVal);
  }

  public static int getSaasPolicyMonitorPoolSize(String value, int defaultVal) {
    if (Strings.isNullOrEmpty(value)) {
      return getIntEnvValueOrDefault(NXIQ_SAAS_POLICY_MONITOR_POOL_SIZE, defaultVal);
    }
    return NumberUtils.toInt(value, defaultVal);
  }

  public static <T> T stringToObject(
      final String value,
      final Class<T> clazz,
      final T defaultValue)
  {
    if (value != null) {
      try {
        return JsonUtils.parse(value, clazz);
      }
      catch (Exception e) {
        log.error(
            "Unable to deserialize configuration '%s' into '%s', using default configuration '%s'.".formatted(value,
                clazz.getName(), objectToString(defaultValue)),
            e);
      }
    }
    return defaultValue;
  }

  public static <T> String objectToString(final T value) {
    if (value == null) {
      return null;
    }
    try {
      return JsonUtils.writeUnformatted(value);
    }
    catch (Exception e) {
      throw new BadRequestException("Unable to serialize configuration '%s'.".formatted(value), e);
    }
  }

  private static int getIntEnvValueOrDefault(@NotNull String env, int defaultVal) {
    String envValue = System.getenv(env);
    return NumberUtils.toInt(envValue, defaultVal);
  }
}
