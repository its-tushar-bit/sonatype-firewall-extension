/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import io.dropwizard.configuration.ConfigurationParsingException;
import io.dropwizard.setup.Bootstrap;
import org.hamcrest.Matcher;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;

public class ConfigurationCheckerTest
{
  @Test
  public void testCheck_NoArguments_DoesNotThrowException() throws Exception {
    new ConfigurationChecker().check(new String[]{}, null);
  }

  @Test
  public void testCheck_OnlyCommand_DoesNotThrowException() throws Exception {
    new ConfigurationChecker().check(new String[]{"server"}, null);
  }

  @Test
  public void testCheck_NonExistantConfig_DoesNotThrowException() throws Exception {
    new ConfigurationChecker().check(new String[]{"server", getAbsolutePath("") + "doesNotExist"}, null);
  }

  @Test
  public void testCheck_ConfigWithHttp_SuggestsUpdateConfig() throws Exception {
    assertSuggestsUpdateConfig("config-with-http.yml");
  }

  @Test
  public void testCheck_ConfigWithLoggingConsole_SuggestsUpdateConfig() throws Exception {
    assertSuggestsUpdateConfig("config-with-logging-console.yml");
  }

  @Test
  public void testCheck_ConfigWithLoggingFile_SuggestsUpdateConfig() throws Exception {
    assertSuggestsUpdateConfig("config-with-logging-file.yml");
  }

  @Test
  public void testCheck_ConfigWithLoggingSyslog_SuggestsUpdateConfig() throws Exception {
    assertSuggestsUpdateConfig("config-with-logging-syslog.yml");
  }

  @Test
  public void testCheck_ConfigWithOtherUnknown_DoesNotSuggestUpdateConfig() throws Exception {
    assertDoesNotSuggestUpdateConfig("config-with-other-unknown.yml");
  }

  @Test
  public void testCheck_ConfigWithServer_DoesNotThrowException() throws Exception {
    new ConfigurationChecker().check(new String[]{"server", getAbsolutePath("config-with-server.yml")},
        new Bootstrap<>(new InsightBrainService()));
  }

  private void assertSuggestsUpdateConfig(String configFileName) {
    assertConfigException(configFileName, RuntimeException.class,
        is(ConfigurationChecker.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE));
  }

  private void assertDoesNotSuggestUpdateConfig(String configFileName) {
    assertConfigException(configFileName, ConfigurationParsingException.class,
        not(ConfigurationChecker.SUGGEST_UPDATE_CONFIG_EXCEPTION_MESSAGE));
  }

  private void assertConfigException(String configFileName, Class exceptionType, Matcher<String> matcher)
  {
    try {
      new ConfigurationChecker()
          .check(new String[]{"server", getAbsolutePath(configFileName)}, new Bootstrap<>(new InsightBrainService()));
      fail("Expected exception");
    }
    catch (Exception ex) {
      assertThat(ex, instanceOf(exceptionType));
      assertThat(ex.getMessage(), matcher);
    }
  }

  private String getAbsolutePath(String fileName) {
    return ConfigurationChecker.class.getResource("/ConfigurationCheckerTest/" + fileName).getFile();
  }
}
