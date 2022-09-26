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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.ClassUtils;
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

  private ConfigurationUtils() { }

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
    String baseUrl = new SystemConfigurationPropertyDAO().get(tx, SystemConfigurationProperty.BASE_URL);
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
      List<String> nullFiltered =
          values.stream().filter(Objects::nonNull)
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
    Boolean migrationEnabled = DatabaseMigrator.isMigrationEnabledFromEnvironmentVariable();
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
    return ObjectUtils.toString(minutes, null);
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
}
