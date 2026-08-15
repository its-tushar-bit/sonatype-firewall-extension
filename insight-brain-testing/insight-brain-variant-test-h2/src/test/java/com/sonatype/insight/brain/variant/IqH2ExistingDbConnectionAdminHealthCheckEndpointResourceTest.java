/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.operational.check.ExistingDbConnectionAdminHealthCheckEndpoint;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ExistingDbConnectionAdminHealthCheckEndpointResourceTest
{
  private IqTestContext ctx;

  @Test
  void testGetHealthCheckResponse() throws Exception {
    HttpResponse httpResponse =
        ctx.adminRequest()
            .path(ctx.lookup(ExistingDbConnectionAdminHealthCheckEndpoint.class).getPath())
            .anon()
            .get();
    ctx.assertResponseStatus(HttpStatus.SC_NO_CONTENT, httpResponse);
  }
}
