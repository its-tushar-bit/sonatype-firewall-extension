/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.LogOutput;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ConfigurationUtilsTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(ConfigurationUtils.class);

  @Test
  public void testUrlValueToString_Null() {
    assertThat(ConfigurationUtils.urlValueToString(null)).isNull();
  }

  @Test
  public void testUrlValueToString_Invalid() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> ConfigurationUtils.urlValueToString("invalid")).withMessageContaining("Invalid URL: invalid/");
  }

  @Test
  public void testUrlValueToString_AddsEndingForwardSlashIfNeeded() {
    assertThat(ConfigurationUtils.urlValueToString("http://url")).isEqualTo("http://url/");
    assertThat(ConfigurationUtils.urlValueToString("http://url/")).isEqualTo("http://url/");
  }

  @Test
  public void testForceBaseUrlToString_True() {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");

    try (TransactionContext tx = new SystemConfigurationPropertyDAO().createTransactionContext()) {
      assertThat(ConfigurationUtils.forceBaseUrlToString(tx, true)).isEqualTo(Boolean.toString(true));
    }

    assertThat(logOutput).atErrorLevel()
        .contains("DEPRECATION NOTICE: Forcing use of server base URL: http://baseUrl/, any 'X-Forwarded-*' headers " +
            "will be ignored. More information at http://links.sonatype.com/products/clm/docs/base-url");
  }

  @Test
  public void testForceBaseUrlToString_False() {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");

    try (TransactionContext tx = new SystemConfigurationPropertyDAO().createTransactionContext()) {
      assertThat(ConfigurationUtils.forceBaseUrlToString(tx, false)).isEqualTo(Boolean.toString(false));
    }

    assertThat(logOutput).atInfoLevel().contains("Server base URL: http://baseUrl/");
  }

  @Test
  public void testForceBaseUrlToString_Null() {
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = new SystemConfigurationPropertyDAO();
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");

    try (TransactionContext tx = new SystemConfigurationPropertyDAO().createTransactionContext()) {
      assertThat(ConfigurationUtils.forceBaseUrlToString(tx, null)).isNull();
    }

    assertThat(logOutput).atInfoLevel().contains("Server base URL: http://baseUrl/");
  }
}
