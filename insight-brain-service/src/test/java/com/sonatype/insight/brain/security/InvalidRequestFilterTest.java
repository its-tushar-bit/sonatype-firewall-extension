/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.client.utils.Authentication;

import com.google.common.net.HttpHeaders;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InvalidRequestFilterTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  public void testBackslashSemicolonBlockedByDefault() throws Exception {
    HttpResponse response = restRequest().path("any/thing/\\after-backslash").get();
    assertResponseStatus(400, response);

    response = restRequest().path("any/thing/;after-semicolon").get();
    assertResponseStatus(400, response);
  }

  @Test
  public void testNonAsciiAllowedByDefault() throws Exception {
    // Jetty 10 handles this now rather than InvalidRequestFilter
    assertThat(doRequestWithNonAsciiCharacters()).isEqualTo(400);
  }

  @Test
  public void testBackslashEnabled() throws Exception {
    ApiConfigurationService configurationService = getCLMServer().getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH, false);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.BLOCK_BACKSLASH_IN_PATH);

    // Jetty 12 handles this now rather than InvalidRequestFilter (blocks "Suspicious Path Character")
    HttpResponse response = restRequest().path("any/thing/\\after-backslash").get();
    assertResponseStatus(400, response);
  }

  @Test
  public void testNonAsciiEnabled() throws Exception {
    ApiConfigurationService configurationService = getCLMServer().getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, false);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH);

    // Jetty 10 handles this now rather than InvalidRequestFilter
    assertThat(doRequestWithNonAsciiCharacters()).isEqualTo(400);
  }

  @Test
  public void testNonAsciiDisabled() throws Exception {
    ApiConfigurationService configurationService = getCLMServer().getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH, true);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.BLOCK_NON_ASCII_IN_PATH);

    assertThat(doRequestWithNonAsciiCharacters()).isEqualTo(400);
  }

  @Test
  public void testSemicolonEnabled() throws Exception {
    ApiConfigurationService configurationService = getCLMServer().getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH, false);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.BLOCK_SEMICOLON_IN_PATH);

    HttpResponse response = restRequest().path("any/thing/;after-backslash").get();
    assertResponseStatus(404, response);
  }

  private int doRequestWithNonAsciiCharacters() throws IOException {
    // This request is done "manually" to avoid the non-ASCII characters be encoded by underlying libraries
    URL url = new URL(getRestBaseUrl() + "any/thing/non-ascii/\u007F/газета");
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    Authentication authentication = getCLMServer().getClientConfiguration().getServerAuth();
    String authenticationString = authentication.getUsername() + ":" + new String(authentication.getPassword());
    String encodedAuthentication =
        Base64.getEncoder().encodeToString(authenticationString.getBytes(StandardCharsets.UTF_8));
    connection.setRequestProperty(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuthentication);

    return connection.getResponseCode();
  }
}
