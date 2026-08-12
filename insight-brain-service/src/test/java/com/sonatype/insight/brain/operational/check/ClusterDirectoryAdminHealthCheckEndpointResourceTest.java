/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Test;

public class ClusterDirectoryAdminHealthCheckEndpointResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testThreadDeadlock() throws Exception {
    HttpResponse httpResponse =
        adminRequest()
            .path(getCLMServer().getInstance(ClusterDirectoryAdminHealthCheckEndpoint.class).getPath())
            .anon()
            .get();
    assertResponseStatus(HttpStatus.SC_NO_CONTENT, httpResponse);
  }
}
