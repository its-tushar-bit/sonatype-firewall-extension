/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import jakarta.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexCacheControlFilterTest
    extends AbstractResourceTest
{
  @Test
  public void testCacheBustingForIndexPage() throws Exception {
    HttpResponse response = restRequest().followRedirects().anon().get();
    assertResponseStatus(200, response);
    assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL))
        .isEqualTo("no-cache, no-store, max-age=0, must-revalidate");
  }
}
