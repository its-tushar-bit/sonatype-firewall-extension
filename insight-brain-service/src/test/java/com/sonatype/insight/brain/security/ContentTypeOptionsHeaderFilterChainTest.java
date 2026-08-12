/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResource;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentTypeOptionsHeaderFilterChainTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  public void testHeader() throws Exception {
    assertHeader(restRequest().path(UserSessionResource.RESOURCE_PATH).post());
    assertHeader(restRequest().path("/assets/index.html").get());
    assertHeader(restRequest().path("/assets/bundle.js").get());
    assertHeader(restRequest().path(DashboardResource.RESOURCE_PATH)
        .path(DashboardResource.GET_VIOLATION_RISKS_EXPORT_PATH)
        .get());
  }

  private void assertHeader(HttpResponse response) {
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
  }
}
