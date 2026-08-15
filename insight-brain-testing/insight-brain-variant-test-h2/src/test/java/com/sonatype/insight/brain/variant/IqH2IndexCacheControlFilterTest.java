/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import jakarta.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2IndexCacheControlFilterTest
{
  private IqTestContext ctx;

  @Test
  void testCacheBustingForIndexPage() throws Exception {
    HttpResponse response = ctx.restRequest().followRedirects().anon().get();
    ctx.assertResponseStatus(200, response);
    assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL))
        .isEqualTo("no-cache, no-store, max-age=0, must-revalidate");
  }
}
