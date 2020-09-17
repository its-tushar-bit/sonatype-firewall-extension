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

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.client.utils.Authentication;

import com.ning.http.util.Base64;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InvalidRequestFilterTest
    extends AbstractBrainServiceTest
{
  @Test
  public void testBackslashSemicolonNonAsciiBlockedByDefault() throws Exception {
    HttpResponse response = restRequest().path("any/thing/\\after-backslash").get();
    assertResponseStatus(400, response);

    assertThat(doRequestWithNonAsciiCharacters()).isEqualTo(400);

    response = restRequest().path("any/thing/;after-semicolon").get();
    assertResponseStatus(400, response);
  }

  @Test
  @ManualServerInit
  public void testBackslashEnabled() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.setBlockBackslashInPath(false);
      }
    });

    HttpResponse response = restRequest().path("any/thing/\\after-backslash").get();
    assertResponseStatus(404, response);
  }

  @Test
  @ManualServerInit
  public void testNonAsciiEnabled() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.setBlockNonAsciiInPath(false);
      }
    });

    assertThat(doRequestWithNonAsciiCharacters()).isEqualTo(404);
  }

  @Test
  @ManualServerInit
  public void testSemicolonEnabled() throws Exception {
    initServer(new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.setBlockSemicolonInPath(false);
      }
    });

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
    String encodedAuthentication = Base64.encode(authenticationString.getBytes(StandardCharsets.UTF_8));
    connection.setRequestProperty(HttpHeaders.Names.AUTHORIZATION, "Basic " + encodedAuthentication);

    return connection.getResponseCode();
  }
}
