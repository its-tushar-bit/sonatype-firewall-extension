/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.21
 */
public class HttpHeaderValidatorFilterChainTest
    extends AbstractBrainServiceIntegrationTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserSessionResource.RESOURCE_PATH);
  }

  @Test
  public void testValidHeader() throws Exception {
    assertResponseStatus(204, restRequest().header("Host", "localhost").post());
  }

  @Test
  public void testInvalidHeader_Proto() throws Exception {
    HttpResponse response = restRequest().header("X-Forwarded-Proto", "http\"><script>alert(document.domain)</script>")
        .post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Illegal header value detected in 'X-Forwarded-Proto'");
  }

  @Test
  public void testInvalidHeader_Host() throws Exception {
    HttpResponse response = restRequest().header("X-Forwarded-Host", "\"><script>alert(document.domain)</script>")
        .post();

    // Jetty 10 now handles this rather than HttpHeaderValidatorFilter (see ForwardRequestCustomizer#onError)
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Bad header value for X-Forwarded-Host");
  }

  @Test
  public void testInvalidHeader_ForwardedProto() throws Exception {
    HttpResponse response = restRequest().header("Forwarded", "proto=http\"><script>alert(document.domain)</script>")
        .post();
    // Jetty 12 now handles this rather than HttpHeaderValidatorFilter (see ForwardRequestCustomizer#onError)
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Bad header value for Forwarded");
  }

  @Test
  public void testInvalidHeader_ForwardedHost() throws Exception {
    HttpResponse response = restRequest().header("Forwarded", "host=\"><script>alert(document.domain)</script>").post();

    // Jetty 10 now handles this rather than HttpHeaderValidatorFilter (see ForwardRequestCustomizer#onError)
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Bad header value for Forwarded");
  }
}
