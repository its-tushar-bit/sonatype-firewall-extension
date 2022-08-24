/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationInfoTest
    extends AbstractComponentTest
{
  @Inject
  private ConfigurationInfo configurationInfo;

  @Test
  public void testGetConfigurationInfo() throws Exception {
    setHdsUrl(null);
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.HDS_URL, "https://clm-staging.sonatype.com/");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.CSRF_PROTECTION, String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.CDN_URL, "http://my-cdn-url/");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES,
        String.valueOf(10));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
        String.valueOf(500));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.USER_AGENT_SUFFIX, "test suffix");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.CSP_ENABLED, String.valueOf(true));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT,
        String.valueOf(20));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD,
        String.valueOf(30));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT,
        String.valueOf(40));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER, ",");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS,
        String.valueOf(50));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS,
        String.valueOf(60));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS,
        String.valueOf(70));
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER, String.valueOf(true));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.POLICY_MONITORING_HOUR, String.valueOf(22));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.DB_BACKUP_DIR, "sonatype-work");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE,
        "test-passphrase");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED,
        String.valueOf(false));
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, String.valueOf(true));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.BASE_URL, "http://127.0.0.1:8070");
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.FORCE_BASE_URL, String.valueOf(true));
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST,
        "[\"*first.com\",\"second.*\"]");

    JsonNode configNode = JsonUtils.parse(configurationInfo.getConfigurationInfo());

    assertThat(configNode.get(SystemConfigurationProperty.HDS_URL).asText()).isEqualTo(
        "https://clm-staging.sonatype.com/");
    assertThat(configNode.get(SystemConfigurationProperty.CSRF_PROTECTION).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.CDN_URL).asText()).isEqualTo("http://my-cdn-url/");
    assertThat(configNode.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES).asText()).isEqualTo("10");
    assertThat(configNode.get(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE).asText()).isEqualTo("500");
    assertThat(configNode.get(SystemConfigurationProperty.USER_AGENT_SUFFIX).asText()).isEqualTo("test suffix");
    assertThat(configNode.get(SystemConfigurationProperty.CSP_ENABLED).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT).asText()).isEqualTo("20");
    assertThat(configNode.get(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD).asText()).isEqualTo(
        "30");
    assertThat(configNode.get(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT).asText()).isEqualTo("40");
    assertThat(configNode.get(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER).asText()).isEqualTo(
        ",");
    assertThat(configNode.get(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS).asText()).isEqualTo("50");
    assertThat(configNode.get(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS).asText()).isEqualTo("60");
    assertThat(configNode.get(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS).asText()).isEqualTo("70");
    assertThat(configNode.get(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER)
        .asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.POLICY_MONITORING_HOUR).asText()).isEqualTo("22");
    assertThat(configNode.get(SystemConfigurationProperty.DB_BACKUP_DIR).asText()).isEqualTo("sonatype-work");
    assertThat(configNode.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE).asText()).isEqualTo(
        "test-passphrase");
    assertThat(configNode.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED).asText()).isEqualTo("false");
    assertThat(configNode.get(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)
        .asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.BASE_URL).asText()).isEqualTo("http://127.0.0.1:8070");
    assertThat(configNode.get(SystemConfigurationProperty.FORCE_BASE_URL).asText()).isEqualTo("true");
    assertThat(configNode.get(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST).asText()).isEqualTo(
        "[\"*first.com\",\"second.*\"]");
  }

  @Test
  public void testGetSourceControlConfigurationInfo_noConfig() throws Exception {
    JsonNode configNode = JsonUtils.parse(configurationInfo.getConfigurationInfo());

    assertThat(configNode.get(SystemConfigurationProperty.HDS_URL).asText()).isEqualTo("http://unknownhost");
    assertThat(configNode.get(SystemConfigurationProperty.CSRF_PROTECTION).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.CDN_URL).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.SUPPORT_READ_LIMIT_BYTES).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.USER_AGENT_SUFFIX).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.CSP_ENABLED).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.RELEASE_GRAPH_CACHE_SIZE).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.LICENSE_LEGAL_HDS_REQUEST_LIMIT).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD).asText()).isEqualTo(
        "null");
    assertThat(configNode.get(SystemConfigurationProperty.MAX_ADVANCED_SEARCH_CLAUSE_COUNT).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.ADVANCED_SEARCH_CSV_EXPORT_DELIMITER).asText()).isEqualTo(
        "null");
    assertThat(configNode.get(SystemConfigurationProperty.CONNECT_TIMEOUT_IN_SECONDS).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.SOCKET_TIMEOUT_IN_SECONDS).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.REPORT_TIMEOUT_IN_SECONDS).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER)
        .asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.POLICY_MONITORING_HOUR).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.DB_BACKUP_DIR).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.WEBHOOK_SECRET_PASSPHRASE).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.EXTERNAL_HYPERLINKS_ALLOWED).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING)
        .asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.BASE_URL).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.FORCE_BASE_URL).asText()).isEqualTo("null");
    assertThat(configNode.get(SystemConfigurationProperty.FRAME_ANCESTORS_ALLOWLIST).asText()).isEqualTo("null");
  }
}
