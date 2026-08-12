/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTelemetryResourceTest
    extends AbstractResourceTest
{
  @Before
  public void setup() {
    getCLMServer().getInstance(PendoCache.class).invalidateAll();
  }

  @Test
  public void testUserTelemetryResource_NamedSingleton() {
    assertThat(UserTelemetryResource.class.isAnnotationPresent(Named.class)).isTrue();
    assertThat(UserTelemetryResource.class.isAnnotationPresent(Singleton.class)).isTrue();
  }

  @Test
  public void testGetJavascript() throws Exception {
    getHdsServer().respondWith("some javascript").atUri("user-telemetry.js");

    HttpResponse response = restRequest()
        .path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.JAVASCRIPT_PATH)
        .get();
    assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("some javascript");
    assertThat(response.getHeader("Content-Type")).isEqualTo("application/javascript");
  }

  @Test
  public void testGetJavascript_error() throws Exception {
    getHdsServer().respondWith("some error message").andStatus(404).atUri("user-telemetry.js");

    HttpResponse response = restRequest()
        .path(UserTelemetryResource.RESOURCE_PATH, UserTelemetryResource.JAVASCRIPT_PATH)
        .get();
    assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("");
  }

  @Test
  public void testProxyGet() throws Exception {
    String contentType = "application/javascript;charset=UTF-8";
    getHdsServer().respondWith("some response")
        .withType(contentType)
        .atUri(PendoService.HDS_TELEMETRY_PATH + "/foo/bar");

    String url = UriBuilder.fromPath(UserTelemetryResource.RESOURCE_PATH)
        .path(UserTelemetryResource.EVENTS_PATH)
        .build(new String[]{"foo/bar"}, false /* encodeSlashInPath */)
        .toString();
    HttpResponse response = restRequest().path(url).get();
    assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("some response");
    assertThat(response.getContentType()).isEqualTo(contentType);
  }

  @Test
  public void testProxyGet_error() throws Exception {
    HttpResponse response = restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events", "foo", "bar").get();
    assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("");
  }

  @Test
  public void testProxyPost() throws Exception {
    getHdsServer().respondWith("some response").atUri(PendoService.HDS_TELEMETRY_PATH + "/foo/bar");

    String url = UriBuilder.fromPath(UserTelemetryResource.RESOURCE_PATH)
        .path(UserTelemetryResource.EVENTS_PATH)
        .build(new String[]{"foo/bar"}, false /* encodeSlashInPath */)
        .toString();
    HttpResponse response = restRequest().path(url).body("Foo").post();
    assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("some response");
  }

  @Test
  public void testProxyPost_error() throws Exception {
    HttpResponse response = restRequest().path(UserTelemetryResource.RESOURCE_PATH, "events", "foo", "bar").post();
    assertResponseStatus(200, response);

    assertThat(response.getBodyText()).isEqualTo("");
  }
}
