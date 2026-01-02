/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class CspHeaderFilterChainTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  public void testHeaders() throws Exception {
    assertHeaders(restRequest().path("/assets/index.html").get());
    assertHeaders(restRequest().path("/assets/version-graph/rm/nexus/index.html").get());
    assertHeaders(restRequest().path("/assets/version-graph/rm/nexus/viewdetails.html").get());

    Application app = tempEntity.newApplicationWithParent("ReportResourceTest_AppId");
    String scanId = mockReport("/CspHeaderFilterChainTest/report");

    // HDS reports should not include the CSP header(s)
    assertNoHeaders(restRequest().path(app.getPublicId(), scanId).path("browseReport").get());
  }

  private void assertHeaders(HttpResponse response) {
    assertThat(response.getHeader("Content-Security-Policy"))
        .isEqualTo("default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:");

    assertThat(response.getHeader("X-XSS-Protection")).isEqualTo("1; mode=block");
  }

  private void assertNoHeaders(HttpResponse response) {
    assertThat(response.getHeader("Content-Security-Policy")).isNull();
    assertThat(response.getHeader("X-XSS-Protection")).isNull();
  }
}
