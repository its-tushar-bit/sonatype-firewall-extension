/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.telemetry.PendoCache;
import com.sonatype.insight.brain.telemetry.PendoService;
import com.sonatype.insight.brain.telemetry.UserTelemetryResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Anonymous access allowed
 */
@IqH2Test
class IqH2UserTelemetryResourceAuthzTest
{
  private IqTestContext ctx;

  @BeforeEach
  void setup() {
    ctx.lookup(PendoCache.class).invalidateAll();
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().anon();
  }

  @Test
  void testGetJavascript() throws Exception {
    ctx.getHdsServer().respondWith("function foo() {}").atUri("user-telemetry.js");
    HttpResponse response = restRequest()
        .path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.JAVASCRIPT_PATH)
        .get();
    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testGetConfig() throws Exception {
    HttpRequest request = restRequest().path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.CONFIG_PATH);
    HttpResponse response = request.anon().get();
    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testGetProxy() throws Exception {
    ctx.hdsRespondWith("").andStatus(204).atUri(PendoService.HDS_TELEMETRY_PATH);
    HttpRequest request = restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events/");

    HttpResponse response = request.anon().get();
    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testPostProxy() throws Exception {
    ctx.hdsRespondWith("").andStatus(204).atUri(PendoService.HDS_TELEMETRY_PATH);
    HttpRequest request = restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events/");

    HttpResponse response = request.anon().post();
    ctx.assertResponseStatus(200, response);
  }
}
