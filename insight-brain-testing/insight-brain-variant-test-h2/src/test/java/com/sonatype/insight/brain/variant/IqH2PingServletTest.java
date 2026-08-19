/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2PingServletTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.PING_RESOURCE_PATH);
  }

  @Test
  void testPing_Licensed() throws Exception {
    HttpResponse response = restRequest().anon().get();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText().trim()).isEqualTo("pong");
  }

  @Test
  void testPing_Unlicensed() throws Exception {
    ctx.uninstallLicense();

    HttpResponse response = restRequest().anon().get();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBodyText().trim()).isEqualTo("pong");
  }
}
