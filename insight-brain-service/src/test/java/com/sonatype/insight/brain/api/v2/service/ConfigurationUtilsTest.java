/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.AllowedIp;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.api.v2.service.ConfigurationUtils.NXIQ_EVENT_BUS_MAX_THREAD_POOL_SIZE;
import static com.sonatype.insight.brain.api.v2.service.ConfigurationUtils.NXIQ_FIREWALL_QUARANTINE_HDS_POOL_SIZE;
import static com.sonatype.insight.brain.api.v2.service.ConfigurationUtils.NXIQ_SAAS_POLICY_MONITOR_POOL_SIZE;
import static com.sonatype.insight.brain.api.v2.service.ConfigurationUtils.NXIQ_SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE;
import static com.sonatype.insight.brain.api.v2.service.ConfigurationUtils.NXIQ_SOURCE_CONTROL_IMPORT_POOL_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@Category(SlowTest.class)
public class ConfigurationUtilsTest
    extends AbstractComponentTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Rule
  public LogOutput logOutput = new LogOutput(ConfigurationUtils.class);

  private static final ObjectMapper JSON =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");

    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      assertThat(ConfigurationUtils.forceBaseUrlToString(tx, true)).isEqualTo(Boolean.toString(true));
    }

    assertThat(logOutput).atErrorLevel()
        .contains("DEPRECATION NOTICE: Forcing use of server base URL: http://baseUrl/, any 'X-Forwarded-*' headers " +
            "will be ignored. More information at http://links.sonatype.com/products/clm/docs/base-url");
  }

  @Test
  public void testForceBaseUrlToString_False() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");

    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      assertThat(ConfigurationUtils.forceBaseUrlToString(tx, false)).isEqualTo(Boolean.toString(false));
    }

    assertThat(logOutput).atInfoLevel().contains("Server base URL: http://baseUrl/");
  }

  @Test
  public void testForceBaseUrlToString_Null() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.BASE_URL, "http://baseUrl/");

    try (TransactionContext tx = systemConfigurationPropertyDAO.createTransactionContext()) {
      assertThat(ConfigurationUtils.forceBaseUrlToString(tx, null)).isNull();
    }

    assertThat(logOutput).atInfoLevel().contains("Server base URL: http://baseUrl/");
  }

  @Test
  public void stringToFrameAncestorsAllowlist_ReturnList() {
    List<String> stringToList = ConfigurationUtils.stringToFrameAncestorsAllowlist("[\"first\",\"second\"]");
    assertEquals(getAllowlist(), stringToList);
  }

  @Test
  public void stringToFrameAncestorsAllowlist_ReturnNullIfConfigurationIsEmpty() {
    assertNull(ConfigurationUtils.stringToFrameAncestorsAllowlist(null));
  }

  @Test(expected = RuntimeException.class)
  public void stringToFrameAncestorsAllowlist_ThrowsException() {
    ConfigurationUtils.stringToFrameAncestorsAllowlist("invalid_value");
  }

  @Test
  public void frameAncestorsAllowlistToString_ReturnsNullIfListIsEmpty() {
    assertNull(ConfigurationUtils.frameAncestorsAllowlistToString(Collections.emptyList()));
  }

  @Test
  public void frameAncestorsAllowlistToString() {
    String listToString = ConfigurationUtils.frameAncestorsAllowlistToString(getAllowlist());
    assertEquals("[\"'self'\",\"first\",\"second\"]", listToString);
  }

  @Test
  public void frameAncestorsAllowListIgnoreAllValuesIfNoneConfigPresents() {
    List<String> allowlist = getAllowlist();
    allowlist.add("'none'");
    String listToString = ConfigurationUtils.frameAncestorsAllowlistToString(allowlist);
    assertEquals("[\"'none'\"]", listToString);
  }

  @Test
  public void frameAncestorsAllowlistToString_RemoveNullValues() {
    List<String> allowlist = getAllowlist();
    allowlist.add(null);
    String listToString = ConfigurationUtils.frameAncestorsAllowlistToString(allowlist);
    assertEquals("[\"'self'\",\"first\",\"second\"]", listToString);
  }

  @Test
  public void frameAncestorsAllowlistToString_RemoveDuplicates() {
    List<String> allowlist = getAllowlist();
    allowlist.add("'self'");
    String listToString = ConfigurationUtils.frameAncestorsAllowlistToString(allowlist);
    assertEquals("[\"'self'\",\"first\",\"second\"]", listToString);
  }

  @Test
  public void frameAncestorsAllowlistToString_ReturnNullIfListContainsOnlyNulls() {
    List<String> allowlist = new ArrayList<>();
    allowlist.add(null);
    allowlist.add(null);
    String listToString = ConfigurationUtils.frameAncestorsAllowlistToString(allowlist);
    assertNull(listToString);
  }

  @Test
  public void frameAncestorsAllowlistToString_ReturnNullIfListIsNull() {
    assertNull(ConfigurationUtils.frameAncestorsAllowlistToString(null));
  }

  private List<String> getAllowlist() {
    List<String> result = new ArrayList<>();
    result.add("first");
    result.add("second");
    return result;
  }

  @Test
  public void testStringToAccessAllowlist() {
    List<AllowedIp> allowlistIPs = ConfigurationUtils.stringToAccessAllowlist(
        "[{\"ipAddress\":\"192.168.33.10\",\"description\":\"Test IPv4 address\"}," +
            "{\"ipAddress\":\"8ed5:9e96:1da1:f53b:587e:9f4d:a7f9:817e\",\"description\":\"Test IPv6 address\"}," +
            "{\"ipAddress\":\"15.177.0.0/18\",\"description\":\"Test IPv4 CIDR\"}," +
            "{\"ipAddress\":\"2600:1f18:3fff:f800::/56\",\"description\":\"Test IPv6 CIDR\"}]");

    assertThat(allowlistIPs).extracting(allowlistIp -> allowlistIp.getIpAddress())
        .containsExactlyInAnyOrder("192.168.33.10", "8ed5:9e96:1da1:f53b:587e:9f4d:a7f9:817e",
            "15.177.0.0/18", "2600:1f18:3fff:f800::/56");
    assertThat(allowlistIPs).extracting(allowlistIp -> allowlistIp.getDescription())
        .containsExactlyInAnyOrder("Test IPv4 address", "Test IPv6 address", "Test IPv4 CIDR",
            "Test IPv6 CIDR");
  }

  @Test
  public void testStringToAccessAllowlist_ReturnNullIfConfigurationIsEmpty() {
    assertNull(ConfigurationUtils.stringToAccessAllowlist(null));
  }

  @Test
  public void testStringToAccessAllowlist_ThrowsException() {
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(
        () -> ConfigurationUtils.stringToAccessAllowlist("invalid_value"))
        .withMessageContaining("Invalid json: invalid_value");
  }

  @Test
  public void testAccessAllowlistToString() {
    String listToString = ConfigurationUtils.accessAllowlistToString(getAllowlistMap(getAccessAllowlist()));
    assertEquals("[{\"ipAddress\":\"192.168.33.10\",\"description\":\"Test IPv4 address\"}," +
        "{\"ipAddress\":\"8ed5:9e96:1da1:f53b:587e:9f4d:a7f9:817e\",\"description\":\"Test IPv6 address\"}," +
        "{\"ipAddress\":\"15.177.0.0/18\",\"description\":\"Test IPv4 CIDR\"}," +
        "{\"ipAddress\":\"2600:1f18:3fff:f800::/56\",\"description\":\"Test IPv6 CIDR\"}]",
        listToString);
  }

  @Test
  public void testAccessAllowlistToString_ReturnsNullIfListIsEmpty() {
    assertNull(ConfigurationUtils.accessAllowlistToString(Collections.emptyList()));
  }

  @Test
  public void testAccessAllowlistToString_ReturnNullIfListIsNull() {
    assertNull(ConfigurationUtils.accessAllowlistToString(null));
  }

  @Test
  public void testAccessAllowlistToString_ThrowBadRequestForNullValues() {
    List<Map<String, String>> allowlist = getAllowlistMap(getAccessAllowlist());
    allowlist.add(null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> ConfigurationUtils.accessAllowlistToString(allowlist))
        .withMessageContaining("Invalid IP addresses: [null]");
  }

  @Test
  public void testAccessAllowlistToString_ThrowBadRequestForInvalidIPs() {
    List<AllowedIp> allowlist = getAccessAllowlist();
    allowlist.add(new AllowedIp("192.168.33.999", "Invalid IPv4 address"));
    allowlist.add(new AllowedIp("192.168.33.1/31", "Invalid IPv4 CIDR"));
    allowlist.add(new AllowedIp("192.160.1.0/12", "Invalid IPv4 CIDR"));
    allowlist.add(new AllowedIp("192.128.1.0/10", "Invalid IPv4 CIDR"));
    allowlist.add(new AllowedIp("192.1.0.0/8", "Invalid IPv4 CIDR"));
    allowlist.add(new AllowedIp("2600:1f18:3fff:f800/56", "Invalid IPv6 address"));
    allowlist.add(new AllowedIp("2600:1f18:3fff:f800::0001/56", "Invalid IPv6 CIDR"));
    allowlist.add(new AllowedIp(null, "Null IP address"));
    allowlist.add(new AllowedIp("", "Empty String"));
    allowlist.add(null);

    allowlist.add(new AllowedIp("192.168.33.1/32", "Valid IPv4 CIDR"));
    allowlist.add(new AllowedIp("192.168.33.0/31", "Valid IPv4 CIDR"));
    allowlist.add(new AllowedIp("192.168.0.0/18", "Valid IPv4 CIDR"));
    allowlist.add(new AllowedIp("192.128.0.0/9", "Valid IPv4 CIDR"));
    allowlist.add(new AllowedIp("192.0.0.0/8", "Valid IPv4 CIDR"));
    allowlist.add(new AllowedIp("2600:1f18:3fff:f800::/56", "Valid IPv6 CIDR"));

    List<Map<String, String>> allowlistMap = getAllowlistMap(allowlist);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> ConfigurationUtils.accessAllowlistToString(allowlistMap))
        .withMessageContaining("Invalid IP addresses: [192.168.33.999, 192.168.33.1/31, 192.160.1.0/12, " +
            "192.128.1.0/10, 192.1.0.0/8, 2600:1f18:3fff:f800/56, 2600:1f18:3fff:f800::0001/56, null, , null]");
  }

  private List<AllowedIp> getAccessAllowlist() {
    List<AllowedIp> result = new ArrayList<>();
    result.add(new AllowedIp("192.168.33.10", "Test IPv4 address"));
    result.add(new AllowedIp("8ed5:9e96:1da1:f53b:587e:9f4d:a7f9:817e", "Test IPv6 address"));
    result.add(new AllowedIp("15.177.0.0/18", "Test IPv4 CIDR"));
    result.add(new AllowedIp("2600:1f18:3fff:f800::/56", "Test IPv6 CIDR"));
    return result;
  }

  private List<Map<String, String>> getAllowlistMap(List<AllowedIp> allowlist) {
    return JSON.convertValue(allowlist, new TypeReference<List<Map<String, String>>>()
    {
    });
  }

  @Test
  public void testUserAgentSuffix_Null() {
    assertThat(ConfigurationUtils.userAgentSuffix(null)).isNull();
  }

  @Test
  public void testUserAgentSuffix_MaxLength() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> ConfigurationUtils.userAgentSuffix(
        StringUtils.repeat("a", ConfigurationUtils.MAX_USER_AGENT_SUFFIX_SIZE + 1)))
        .withMessageContaining(
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

  @Test
  public void testSessionTimeoutToStringWithMinorAcceptableValue() {
    String result = ConfigurationUtils.sessionTimeoutToString(3);
    assertEquals("3", result);
  }

  @Test
  public void testSessionTimeoutToStringWithMajorAcceptableValue() {
    String result = ConfigurationUtils.sessionTimeoutToString(120);
    assertEquals("120", result);
  }

  @Test
  public void testSessionTimeoutToString_WithLowValue() {
    assertThatThrownBy(() -> {
      ConfigurationUtils.sessionTimeoutToString(2);
    }).isInstanceOf(RuntimeException.class)
        .hasMessageEndingWith("Timeout configuration should be in range from 3 to 120 minutes.");
  }

  @Test
  public void testSessionTimeoutToString_WithHighValue() {
    assertThatThrownBy(() -> {
      ConfigurationUtils.sessionTimeoutToString(122);
    }).isInstanceOf(RuntimeException.class)
        .hasMessageEndingWith("Timeout configuration should be in range from 3 to 120 minutes.");
  }

  @Test
  public void testParseRepositoryList() {
    assertThat(ConfigurationUtils.parseRepositoryList("repo1,repo2")).isEqualTo("repo1,repo2");
  }

  @Test
  public void testParseRepositoryList_invalidCharacters() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> ConfigurationUtils.parseRepositoryList("repo1,repo 2, repo3"))
        .withMessageContaining(String.format(ConfigurationUtils.INVALID_CHARACTERS_ERROR_MSG, "repo 2"));
  }

  @Test
  public void testParseRepositoryList_removeInvalidComma() {
    assertThat(ConfigurationUtils.parseRepositoryList("repo1,repo2,,,,,")).isEqualTo("repo1,repo2");
  }

  @Test
  public void testParseRepositoryList_duplicatedItems() {
    assertThat(ConfigurationUtils.parseRepositoryList("repo1,repo1,repo2")).isEqualTo("repo1,repo2");
  }

  @Test
  public void testPurgeScanFiles_InvalidValue() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> ConfigurationUtils.purgeScanFiles("foo-bar"))
        .withMessageContaining(String.format(ConfigurationUtils.INVALID_PURGE_SCAN_FILES_VALUE_MSG, "foo-bar"));
  }

  @Test
  public void testPurgeScanFiles_nullValue() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> ConfigurationUtils.purgeScanFiles(null))
        .withMessageContaining(String.format(ConfigurationUtils.INVALID_PURGE_SCAN_FILES_VALUE_MSG, (String) null));
  }

  @Test
  public void testPurgeScanFiles_withReports() {
    assertThat(ConfigurationUtils.purgeScanFiles(ConfigurationUtils.WITH_REPORTS))
        .isEqualTo(ConfigurationUtils.WITH_REPORTS);
  }

  @Test
  public void testValidateCustomMessage_Null() {
    assertThat(ConfigurationUtils.validateCustomMessage(null)).isNull();
  }

  @Test
  public void testValidateCustomMessage_MaxLength() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> ConfigurationUtils.validateCustomMessage(
        StringUtils.repeat("a", ConfigurationUtils.MAX_QUARANTINED_ITEM_CUSTOM_MESSAGE_SIZE + 1)))
        .withMessageContaining(
            String.format(ConfigurationUtils.LONG_QUARANTINED_ITEM_CUSTOM_MESSAGE_ERROR_MSG,
                ConfigurationUtils.MAX_QUARANTINED_ITEM_CUSTOM_MESSAGE_SIZE));
  }

  @Test
  public void testValidateCustomMessage_exactMaxLength() {
    String maxMessage = StringUtils.repeat("a", ConfigurationUtils.MAX_QUARANTINED_ITEM_CUSTOM_MESSAGE_SIZE);
    assertThat(ConfigurationUtils.validateCustomMessage(maxMessage)).isEqualTo(maxMessage);
  }

  @Test
  public void testValidateCustomMessage_validMessage() {
    assertThat(ConfigurationUtils.validateCustomMessage("Contact security@acme.com"))
        .isEqualTo("Contact security@acme.com");
  }

  @Test
  public void testValidateCustomMessage_withNewlines() {
    String message = "Line 1\nLine 2\nLine 3";
    assertThat(ConfigurationUtils.validateCustomMessage(message)).isEqualTo(message);
  }

  @Test
  public void testValidateCustomMessage_withCRLF_normalizedToLF() {
    String message = "Line 1\r\nLine 2\r\nLine 3";
    assertThat(ConfigurationUtils.validateCustomMessage(message)).isEqualTo("Line 1\nLine 2\nLine 3");
  }

  @Test
  public void testValidateCustomMessage_withCR_normalizedToLF() {
    String message = "Line 1\rLine 2\rLine 3";
    assertThat(ConfigurationUtils.validateCustomMessage(message)).isEqualTo("Line 1\nLine 2\nLine 3");
  }

  @Test
  public void testValidateCustomMessage_withUnicode() {
    String message = "Komponentti karanteenissa. Ota yhteytt\u00e4 turvallisuus@yritys.fi";
    assertThat(ConfigurationUtils.validateCustomMessage(message)).isEqualTo(message);
  }

  @Test
  public void testValidateCustomMessage_withUrl() {
    String message = "Visit https://security.acme.com/waivers to request a waiver";
    assertThat(ConfigurationUtils.validateCustomMessage(message)).isEqualTo(message);
  }

  @Test
  public void testValidateCustomMessage_emptyString() {
    assertThat(ConfigurationUtils.validateCustomMessage("")).isEqualTo("");
  }

  @Test
  public void testValidateCustomMessage_htmlTags_allowed() {
    String message = "Contact <security@acme.com> for help";
    assertThat(ConfigurationUtils.validateCustomMessage(message)).isEqualTo(message);
  }

  @Test
  public void testValidateCustomMessage_controlCharacters_rejected() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> ConfigurationUtils.validateCustomMessage("text\u0000null byte"))
        .withMessageContaining(ConfigurationUtils.CUSTOM_MESSAGE_CONTROL_CHARS_ERROR_MSG);
  }

  @Test
  public void testValidateCustomMessage_ansiEscapeCodes_rejected() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> ConfigurationUtils.validateCustomMessage("normal \u001b[31mred text\u001b[0m"))
        .withMessageContaining(ConfigurationUtils.CUSTOM_MESSAGE_CONTROL_CHARS_ERROR_MSG);
  }

  @Test
  public void testValidateCustomMessage_bellCharacter_rejected() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> ConfigurationUtils.validateCustomMessage("alert\u0007message"))
        .withMessageContaining(ConfigurationUtils.CUSTOM_MESSAGE_CONTROL_CHARS_ERROR_MSG);
  }

  @Test
  public void testValidateCustomMessage_tabCharacter_rejected() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> ConfigurationUtils.validateCustomMessage("col1\tcol2"))
        .withMessageContaining(ConfigurationUtils.CUSTOM_MESSAGE_CONTROL_CHARS_ERROR_MSG);
  }

  @Test
  public void testGetEventBusThreadPoolSize_withValue() {
    assertThat(ConfigurationUtils.getEventBusThreadPoolSize("5", 10)).isEqualTo(5);
  }

  @Test
  public void testGetEventBusThreadPoolSize_withoutValue() {
    assertThat(ConfigurationUtils.getEventBusThreadPoolSize("", 10)).isEqualTo(10);
  }

  @Test
  public void testGetEventBusThreadPoolSize_withoutValueWithConfigAsGlobal() {
    environmentVariables.set(NXIQ_EVENT_BUS_MAX_THREAD_POOL_SIZE, "30");

    assertThat(ConfigurationUtils.getEventBusThreadPoolSize("", 10)).isEqualTo(30);
  }

  @Test
  public void testGetSourceControlEventProcessorThreadSize_withValue() {
    assertThat(ConfigurationUtils.getSourceControlEventProcessorPoolSize("5", 10)).isEqualTo(5);
  }

  @Test
  public void testGetSourceControlEventProcessorThreadSize_withoutValue() {
    assertThat(ConfigurationUtils.getSourceControlEventProcessorPoolSize("", 10)).isEqualTo(10);
  }

  @Test
  public void testGetSourceControlEventProcessorThreadSize_withoutValueWithEnvConfig() {
    environmentVariables.set(NXIQ_SOURCE_CONTROL_EVENT_PROCESSOR_POOL_SIZE, "30");

    assertThat(ConfigurationUtils.getSourceControlEventProcessorPoolSize("", 10)).isEqualTo(30);
  }

  @Test
  public void testGetSourceControlImportThreadSize_withValue() {
    assertThat(ConfigurationUtils.getSourceControlImportPoolSize("5", 10)).isEqualTo(5);
  }

  @Test
  public void testGetSourceControlImportThreadSize_withoutValue() {
    assertThat(ConfigurationUtils.getSourceControlImportPoolSize("", 10)).isEqualTo(10);
  }

  @Test
  public void testGetSourceControlImportThreadSize_withoutValueWithEnvConfig() {
    environmentVariables.set(NXIQ_SOURCE_CONTROL_IMPORT_POOL_SIZE, "30");

    assertThat(ConfigurationUtils.getSourceControlImportPoolSize("", 10)).isEqualTo(30);
  }

  @Test
  public void testGetSaasPolicyMonitorMaxPoolSize_withValue() {
    assertThat(ConfigurationUtils.getSaasPolicyMonitorPoolSize("5", 10)).isEqualTo(5);
  }

  @Test
  public void testGetSaasPolicyMonitorMaxPoolSize_withoutValue() {
    assertThat(ConfigurationUtils.getSaasPolicyMonitorPoolSize("", 10)).isEqualTo(10);
  }

  @Test
  public void testGetSaasPolicyMonitorMaxPoolSize_withValueWithEnvConfig() {
    environmentVariables.set(NXIQ_SAAS_POLICY_MONITOR_POOL_SIZE, "30");

    assertThat(ConfigurationUtils.getSaasPolicyMonitorPoolSize("5", 10)).isEqualTo(5);
  }

  @Test
  public void testGetSaasPolicyMonitorMaxPoolSize_withoutValueWithEnvConfig() {
    environmentVariables.set(NXIQ_SAAS_POLICY_MONITOR_POOL_SIZE, "30");

    assertThat(ConfigurationUtils.getSaasPolicyMonitorPoolSize("", 10)).isEqualTo(30);
  }

  @Test
  public void testGetFirewallQuarantineHdsPoolSize_withInRangeValue() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsPoolSize("30", 20)).isEqualTo(30);
  }

  @Test
  public void testGetFirewallQuarantineHdsPoolSize_withoutValue() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsPoolSize("", 20)).isEqualTo(20);
  }

  @Test
  public void testGetFirewallQuarantineHdsPoolSize_withEnvVar() {
    environmentVariables.set(NXIQ_FIREWALL_QUARANTINE_HDS_POOL_SIZE, "40");

    assertThat(ConfigurationUtils.getFirewallQuarantineHdsPoolSize("", 20)).isEqualTo(40);
  }

  @Test
  public void testGetFirewallQuarantineHdsPoolSize_zeroFallsBackToDefault() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsPoolSize("0", 20)).isEqualTo(20);
    assertThat(logOutput).atWarnLevel().contains("out of range");
  }

  @Test
  public void testGetFirewallQuarantineHdsPoolSize_aboveMaxFallsBackToDefault() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsPoolSize("51", 20)).isEqualTo(20);
    assertThat(logOutput).atWarnLevel().contains("out of range");
  }

  @Test
  public void testGetFirewallQuarantineHdsConnectTimeoutInSeconds_withInRangeValue() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsConnectTimeoutInSeconds("30", 10)).isEqualTo(30);
  }

  @Test
  public void testGetFirewallQuarantineHdsConnectTimeoutInSeconds_zeroFallsBackToDefault() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsConnectTimeoutInSeconds("0", 10)).isEqualTo(10);
    assertThat(logOutput).atWarnLevel().contains("out of range");
  }

  @Test
  public void testGetFirewallQuarantineHdsConnectTimeoutInSeconds_aboveMaxFallsBackToDefault() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsConnectTimeoutInSeconds("61", 10)).isEqualTo(10);
    assertThat(logOutput).atWarnLevel().contains("out of range");
  }

  @Test
  public void testGetFirewallQuarantineHdsSocketTimeoutInSeconds_withInRangeValue() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsSocketTimeoutInSeconds("30", 20)).isEqualTo(30);
  }

  @Test
  public void testGetFirewallQuarantineHdsSocketTimeoutInSeconds_zeroFallsBackToDefault() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsSocketTimeoutInSeconds("0", 20)).isEqualTo(20);
    assertThat(logOutput).atWarnLevel().contains("out of range");
  }

  @Test
  public void testGetFirewallQuarantineHdsSocketTimeoutInSeconds_aboveMaxFallsBackToDefault() {
    assertThat(ConfigurationUtils.getFirewallQuarantineHdsSocketTimeoutInSeconds("61", 20)).isEqualTo(20);
    assertThat(logOutput).atWarnLevel().contains("out of range");
  }
}
