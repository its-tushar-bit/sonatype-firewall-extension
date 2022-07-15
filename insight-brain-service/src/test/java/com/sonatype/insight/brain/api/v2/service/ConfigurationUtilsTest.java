/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

  @Test
  public void getStringToList_ReturnList() {
    List<String> stringToList = ConfigurationUtils.getStringToList("[\"first\",\"second\"]");
    assertEquals(getAllowlist(), stringToList);
  }

  @Test
  public void getStringToList_ReturnNullIfConfigurationIsEmpty() {
    assertNull(ConfigurationUtils.getStringToList(null));
  }

  @Test(expected = RuntimeException.class)
  public void getStringToList_ThrowsException() {
    ConfigurationUtils.getStringToList("invalid_value");
  }

  @Test
  public void getListToString_ReturnsNullIfListIsEmpty() {
    assertNull(ConfigurationUtils.getListToString(Collections.emptyList()));
  }

  @Test
  public void getListToString() {
    String listToString = ConfigurationUtils.getListToString(getAllowlist());
    assertEquals("[\"first\",\"second\"]", listToString);
  }

  @Test
  public void getListToString_RemoveNullValues() {
    List<String> allowlist = getAllowlist();
    allowlist.add(null);
    String listToString = ConfigurationUtils.getListToString(allowlist);
    assertEquals("[\"first\",\"second\"]", listToString);
  }

  private List<String> getAllowlist() {
    List<String> result = new ArrayList<>();
    result.add("first");
    result.add("second");
    return result;
  }

  @Test
  public void testUserAgentSuffix_Null() {
    assertThat(ConfigurationUtils.userAgentSuffix(null)).isNull();
  }

  @Test
  public void testUserAgentSuffix_MaxLength() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> ConfigurationUtils.userAgentSuffix(
        StringUtils.repeat("a", ConfigurationUtils.MAX_USER_AGENT_SUFFIX_SIZE + 1))).withMessageContaining(
        String.format(ConfigurationUtils.LONG_USER_AGENT_SUFFIX_ERROR_MSG,
            ConfigurationUtils.MAX_USER_AGENT_SUFFIX_SIZE));
  }

  @Test
  public void testUserAgentSuffix_NoControlCharactersToBlockHeaderInjection() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> ConfigurationUtils.userAgentSuffix("\nInjected-Header: Value"))
        .withMessageContaining(ConfigurationUtils.INVALID_USER_AGENT_SUFFIX_ERROR_MSG);
  }

  @Test
  public void testUserAgentSuffix() {
    String validUserAgentSuffix = "Valid User Agent Suffix (Custom/1.0, Bla)";
    assertThat(ConfigurationUtils.userAgentSuffix(validUserAgentSuffix)).isEqualTo(validUserAgentSuffix);
  }
}
