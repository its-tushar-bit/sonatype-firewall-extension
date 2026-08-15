/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AssetPaths;
import com.sonatype.insight.brain.version.VersionResource;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2TraceMethodBlockFilterTest
{
  private IqTestContext ctx;

  @Test
  void testTraceMethodBlocked() throws Exception {
    HttpResponse response = ctx.restRequest().path(AssetPaths.BRAIN_ASSET_PATH, "index.html").send("TRACE");
    ctx.assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);

    response = ctx.restRequest().path(VersionResource.RESOURCE_PATH).send("TRACE");
    ctx.assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);
  }

  @Test
  void testTrackMethodBlocked() throws Exception {
    HttpResponse response = ctx.restRequest().path(AssetPaths.BRAIN_ASSET_PATH, "index.html").send("TRACK");
    ctx.assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);

    response = ctx.restRequest().path(VersionResource.RESOURCE_PATH).send("TRACK");
    ctx.assertResponseStatus(HttpStatus.METHOD_NOT_ALLOWED_405, response);
  }
}
