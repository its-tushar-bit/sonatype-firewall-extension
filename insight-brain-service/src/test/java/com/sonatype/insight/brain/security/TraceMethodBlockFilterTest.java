/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AssetPaths;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.brain.version.VersionResource;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;

public class TraceMethodBlockFilterTest
    extends AbstractBrainServiceIntegrationTest
{
  @Test
  public void testTraceMethodBlocked() throws Exception {
    HttpResponse response = restRequest().path(AssetPaths.BRAIN_ASSET_PATH, "index.html").send("TRACE");
    assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);

    response = restRequest().path(VersionResource.RESOURCE_PATH).send("TRACE");
    assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);
  }

  @Test
  public void testTrackMethodBlocked() throws Exception {
    HttpResponse response = restRequest().path(AssetPaths.BRAIN_ASSET_PATH, "index.html").send("TRACK");
    assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);

    response = restRequest().path(VersionResource.RESOURCE_PATH).send("TRACK");
    assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);
  }
}
