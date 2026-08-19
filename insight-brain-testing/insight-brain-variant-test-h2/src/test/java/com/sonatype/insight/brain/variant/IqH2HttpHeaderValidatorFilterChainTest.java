/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.security.UserSessionResource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.21
 */
@IqH2Test
class IqH2HttpHeaderValidatorFilterChainTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(UserSessionResource.RESOURCE_PATH);
  }

  @Test
  void testValidHeader() throws Exception {
    ctx.assertResponseStatus(204, restRequest().header("Host", "localhost").post());
  }

  @Test
  void testInvalidHeader_Proto() throws Exception {
    HttpResponse response = restRequest().header("X-Forwarded-Proto", "http\"><script>alert(document.domain)</script>")
        .post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Illegal header value detected in 'X-Forwarded-Proto'");
  }

  @Test
  void testInvalidHeader_Host() throws Exception {
    HttpResponse response = restRequest().header("X-Forwarded-Host", "\"><script>alert(document.domain)</script>")
        .post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Illegal header value detected in 'X-Forwarded-Host'");
  }

  @Test
  void testInvalidHeader_ForwardedProto() throws Exception {
    HttpResponse response = restRequest().header("Forwarded", "proto=http\"><script>alert(document.domain)</script>")
        .post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Illegal header value detected in 'Forwarded'");
  }

  @Test
  void testInvalidHeader_ForwardedHost() throws Exception {
    HttpResponse response = restRequest().header("Forwarded", "host=\"><script>alert(document.domain)</script>").post();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Illegal header value detected in 'Forwarded'");
  }
}
