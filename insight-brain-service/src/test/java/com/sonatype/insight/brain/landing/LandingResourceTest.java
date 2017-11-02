/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class LandingResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testHome() throws Exception {
    HttpResponse response = restRequest().anon().get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location"), startsWith(restRequest().getUrl()));
  }

  @Test
  public void testHome_XForwardedProto() throws Exception {
    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "https";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location"), startsWith(xForwardedProto));
  }

  @Test
  public void testHome_XForwardedHost() throws Exception {
    HttpRequest httpRequest = restRequest();
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location"), startsWith("http://" + xForwardedHost));
  }

  @Test
  public void testHome_XForwardedProtoAndXForwardedHost() throws Exception {
    HttpRequest httpRequest = restRequest();
    String xForwardedProto = "https";
    String xForwardedHost = "xforwardedhost:88";
    httpRequest.header("X-Forwarded-Proto", xForwardedProto);
    httpRequest.header("X-Forwarded-Host", xForwardedHost);
    HttpResponse response = httpRequest.get();
    assertResponseStatus(303, response);
    assertThat(response.getHeader("Location"), startsWith(xForwardedProto + "://" + xForwardedHost));
  }
}
