/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.security.UserSessionResource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ContentTypeOptionsHeaderFilterChainTest
{
  private IqTestContext ctx;

  @Test
  void testHeader() throws Exception {
    assertHeader(ctx.restRequest().path(UserSessionResource.RESOURCE_PATH).post());
    assertHeader(ctx.restRequest().path("/assets/index.html").get());
    assertHeader(ctx.restRequest().path("/assets/bundle.js").get());
    assertHeader(ctx.restRequest()
        .path(DashboardResource.RESOURCE_PATH)
        .path(DashboardResource.GET_VIOLATION_RISKS_EXPORT_PATH)
        .get());
  }

  private void assertHeader(HttpResponse response) {
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
  }
}
