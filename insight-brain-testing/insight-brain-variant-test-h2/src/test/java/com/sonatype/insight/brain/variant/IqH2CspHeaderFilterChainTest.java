/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2CspHeaderFilterChainTest
{
  private IqTestContext ctx;

  @Test
  void testHeaders() throws Exception {
    assertHeaders(ctx.restRequest().path("/assets/index.html").get());
    assertHeaders(ctx.restRequest().path("/assets/version-graph/rm/nexus/index.html").get());
    assertHeaders(ctx.restRequest().path("/assets/version-graph/rm/nexus/viewdetails.html").get());

    Application app = ctx.tempEntity().newApplicationWithParent("ReportResourceTest_AppId");
    String scanId = mockReport("/CspHeaderFilterChainTest/report");

    // HDS reports should not include the CSP header(s)
    assertNoHeaders(ctx.restRequest().path(app.getPublicId(), scanId).path("browseReport").get());
  }

  private String mockReport(String resourceName) {
    String scanId = TemporaryEntity.uuid();
    ctx.mockReport(scanId, resourceName);
    return scanId;
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
