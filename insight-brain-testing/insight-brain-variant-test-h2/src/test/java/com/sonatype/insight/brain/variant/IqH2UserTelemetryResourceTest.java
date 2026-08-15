/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.telemetry.PendoCache;
import com.sonatype.insight.brain.telemetry.PendoService;
import com.sonatype.insight.brain.telemetry.UserTelemetryResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2UserTelemetryResourceTest
{
  private IqTestContext ctx;

  @BeforeEach
  void setup() {
    ctx.lookup(PendoCache.class).invalidateAll();
  }

  @Test
  void testUserTelemetryResource_NamedSingleton() {
    assertThat(UserTelemetryResource.class.isAnnotationPresent(Named.class)).isTrue();
    assertThat(UserTelemetryResource.class.isAnnotationPresent(Singleton.class)).isTrue();
  }

  @Test
  void testGetJavascript() throws Exception {
    ctx.getHdsServer().respondWith("some javascript").atUri("user-telemetry.js");

    HttpResponse response = ctx.restRequest()
        .path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.JAVASCRIPT_PATH)
        .get();
    ctx.assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("some javascript");
    assertThat(response.getHeader("Content-Type")).isEqualTo("application/javascript");
  }

  @Test
  void testGetJavascript_error() throws Exception {
    ctx.getHdsServer().respondWith("some error message").andStatus(404).atUri("user-telemetry.js");

    HttpResponse response = ctx.restRequest()
        .path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.JAVASCRIPT_PATH)
        .get();
    ctx.assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("");
  }

  @Test
  void testProxyGet() throws Exception {
    String contentType = "application/javascript;charset=UTF-8";
    ctx.getHdsServer()
        .respondWith("some response")
        .withType(contentType)
        .atUri(PendoService.HDS_TELEMETRY_PATH + "/foo/bar");

    String url = UriBuilder.fromPath(UserTelemetryResource.RESOURCE_PATH)
        .path(UserTelemetryResource.EVENTS_PATH)
        .build(new String[]{"foo/bar"}, false /* encodeSlashInPath */)
        .toString();
    HttpResponse response = ctx.restRequest().path(url).get();
    ctx.assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("some response");
    assertThat(response.getContentType()).isEqualTo(contentType);
  }

  @Test
  void testProxyGet_error() throws Exception {
    HttpResponse response =
        ctx.restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events", "foo", "bar").get();
    ctx.assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("");
  }

  @Test
  void testProxyPost() throws Exception {
    ctx.getHdsServer().respondWith("some response").atUri(PendoService.HDS_TELEMETRY_PATH + "/foo/bar");

    String url = UriBuilder.fromPath(UserTelemetryResource.RESOURCE_PATH)
        .path(UserTelemetryResource.EVENTS_PATH)
        .build(new String[]{"foo/bar"}, false /* encodeSlashInPath */)
        .toString();
    HttpResponse response = ctx.restRequest().path(url).body("Foo").post();
    ctx.assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("some response");
  }

  @Test
  void testProxyPost_error() throws Exception {
    HttpResponse response =
        ctx.restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events", "foo", "bar").post();
    ctx.assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("");
  }
}
