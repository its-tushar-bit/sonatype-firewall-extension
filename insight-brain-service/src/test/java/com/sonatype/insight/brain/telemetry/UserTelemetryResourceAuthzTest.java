/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;

/**
 * Anonymous access allowed
 */
public class UserTelemetryResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Before
  public void setup() {
    getCLMServer().getInstance(PendoCache.class).invalidateAll();
  }

  @Test
  public void testGetJavascript() throws Exception {
    getHdsServer().respondWith("function foo() {}").atUri("user-telemetry.js");
    HttpResponse response = restRequest()
        .path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.JAVASCRIPT_PATH)
        .get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetConfig() throws Exception {
    HttpRequest request = restRequest().path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.CONFIG_PATH);
    HttpResponse response = request.anon().get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testGetProxy() throws Exception {
    hdsRespondWith("").andStatus(204).atUri(PendoService.HDS_TELEMETRY_PATH);
    HttpRequest request = restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events/");

    HttpResponse response = request.anon().get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testPostProxy() throws Exception {
    hdsRespondWith("").andStatus(204).atUri(PendoService.HDS_TELEMETRY_PATH);
    HttpRequest request = restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events/");

    HttpResponse response = request.anon().post();
    assertResponseStatus(200, response);
  }
}
