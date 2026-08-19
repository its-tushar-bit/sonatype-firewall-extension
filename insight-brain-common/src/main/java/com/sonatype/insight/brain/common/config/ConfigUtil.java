/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.config;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ConfigUtil
{
  /**
   * Get the configuration value from the system properties or environment variable or will fallback to the default
   * value.
   *
   * @param key - the configuration key
   * @param defaultValue - value to use if no system property or environment variable is set
   * @return configuration value
   */
  public static String getConfig(String key, String defaultValue) {
    String configValue = System.getProperty(key);
    if (isBlank(configValue)) {
      configValue = System.getenv(key);
      if (isBlank(configValue)) {
        configValue = defaultValue;
      }
    }
    return configValue;

  }

  public static int getIntegerConfig(final String key, final int defaultValue) {
    return Integer.parseInt(getConfig(key, String.valueOf(defaultValue)));
  }

  public static boolean getBooleanConfig(final String key, final boolean defaultValue) {
    return Boolean.parseBoolean(getConfig(key, String.valueOf(defaultValue)));
  }
}
