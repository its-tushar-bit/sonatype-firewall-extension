/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.config;

import java.util.Map.Entry;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigUtilTest
{
  @Test
  public void testGetConfig_systemProperty() {
    String key = "testGetConfig_systemProperty_key";
    String value = "testGetConfig_systemProperty_value";

    try {
      System.setProperty(key, value);
      String configValue = ConfigUtil.getConfig(key, "not-this");
      assertThat(configValue).isEqualTo(value);
    }
    finally {
      System.clearProperty(key);
    }
  }

  @Test
  public void testGetConfig_environmentVariable() {
    Entry<String, String> env = System.getenv().entrySet().stream().findFirst().get();
    String configValue = ConfigUtil.getConfig(env.getKey(), "not-this");
    assertThat(configValue).isEqualTo(env.getValue());
  }

  @Test
  public void testGetConfig_fallbackToDefaultWhenNoSystemPropertyOrEnvironmentVariable() {
    String key = "testFallbackToDefault_key";
    String value = "testFallbackToDefault_value";

    String configValue = ConfigUtil.getConfig(key, value);
    assertThat(configValue).isEqualTo(value);
  }

  @Test
  public void testGetConfig_Integer() {
    String key = "testGetConfig_IntegerKey";
    int value = 10;

    int configValue = ConfigUtil.getIntegerConfig(key, value);
    assertThat(configValue).isEqualTo(value);
  }

  @Test
  public void testGetConfig_Boolean() {
    String key = "testGetConfig_Boolean";
    boolean value = true;

    boolean configValue = ConfigUtil.getBooleanConfig(key, value);
    assertThat(configValue).isEqualTo(value);
  }
}
