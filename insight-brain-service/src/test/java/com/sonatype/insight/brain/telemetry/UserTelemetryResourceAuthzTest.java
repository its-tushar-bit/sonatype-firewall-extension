/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;

public class UserTelemetryResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Before
  public void setup() {
    getCLMServer().getInstance(PendoCache.class).invalidate();
  }

  @Test
  public void testGetJavascript() throws Exception {
    getHdsServer().respondWith("function foo() {}").atUri(PendoCache.HDS_PENDO_JS_PATH);
    HttpResponse response = restRequest()
        .path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.JAVASCRIPT_PATH).get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetConfig() throws Exception {
    testAuthcGet(restRequest().path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.CONFIG_PATH));
  }

  @Test
  public void testGetProxy() throws Exception {
    hdsRespondWith("").andStatus(204).atUri(PendoService.HDS_TELEMETRY_PATH);
    testAuthcGet(restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events/"));
  }

  @Test
  public void testPostProxy() throws Exception {
    hdsRespondWith("").andStatus(204).atUri(PendoService.HDS_TELEMETRY_PATH);
    testAuthcPost(restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events/"));
  }
}
