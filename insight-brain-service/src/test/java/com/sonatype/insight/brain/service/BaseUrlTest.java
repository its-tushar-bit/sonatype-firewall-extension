/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;

import static org.eclipse.jetty.http.HttpHeaders.X_FORWARDED_PROTO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseUrlTest
{
  @Test
  public void testGet_BaseUrlNotSet() throws Exception {
    testGet_BaseUrlNotSet(null);

    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUriBuilder()).thenThrow(new IllegalStateException());
    testGet_BaseUrlNotSet(uriInfo);
  }

  private void testGet_BaseUrlNotSet(UriInfo uriInfo) {
    final InsightConfig appConfig = new InsightConfig();

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo, null);
    try {
      baseUrl.get();
      fail("un set baseUrl should fail");
    }
    catch (IllegalStateException e) {
      assertEquals(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED, e.getMessage());
    }
  }

  @Test
  public void testGet_UsesBaseUri() {
    InsightConfig appConfig = new InsightConfig();
    appConfig.setBaseUrl("http://localhost:8070");

    UriInfo uriInfo = mock(UriInfo.class);

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo, null);
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com:8080")));
    assertEquals("http://clm.sonatype.com:8080/", baseUrl.get());
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com/")));
    assertEquals("http://clm.sonatype.com/", baseUrl.get());
  }

  @Test
  public void testGet_UsesBaseUri_WithXForwardedProtoSet() {
    InsightConfig appConfig = new InsightConfig();
    appConfig.setBaseUrl("http://localhost:8070");

    UriInfo uriInfo = mock(UriInfo.class);
    HttpHeaders httpHeaders = mock(HttpHeaders.class);
    when(httpHeaders.getRequestHeader(X_FORWARDED_PROTO)).thenReturn(Collections.singletonList("https"));

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo, httpHeaders);
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com:8080")));
    assertEquals("https://clm.sonatype.com:8080/", baseUrl.get());
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com/")));
    assertEquals("https://clm.sonatype.com/", baseUrl.get());
  }

  @Test
  public void testGet_UsesBaseUri_WithMultipleXForwardedProtosSet() {
    InsightConfig appConfig = new InsightConfig();
    appConfig.setBaseUrl("http://localhost:8070");

    UriInfo uriInfo = mock(UriInfo.class);
    HttpHeaders httpHeaders = mock(HttpHeaders.class);
    when(httpHeaders.getRequestHeader(X_FORWARDED_PROTO)).thenReturn(Arrays.asList("https", "http"));

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo, httpHeaders);
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com:8080")));
    assertEquals("https://clm.sonatype.com:8080/", baseUrl.get());
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com/")));
    assertEquals("https://clm.sonatype.com/", baseUrl.get());
  }

  @Test
  public void testGet_UsesInsightConfigBaseUrl() throws Exception {
    testGet_UsesInsightConfigBaseUrl(null);

    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUriBuilder()).thenThrow(new IllegalStateException());
    testGet_UsesInsightConfigBaseUrl(uriInfo);
  }

  private void testGet_UsesInsightConfigBaseUrl(UriInfo uriInfo) {
    InsightConfig appConfig = new InsightConfig();

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo, null);
    appConfig.setBaseUrl("http://test.sonatype.com");
    assertEquals("http://test.sonatype.com/", baseUrl.get());
    appConfig.setBaseUrl("http://test.sonatype.com/");
    assertEquals("http://test.sonatype.com/", baseUrl.get());
  }

  @Test
  public void testRedirect() {
    InsightConfig appConfig = new InsightConfig();

    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com:8080")));

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo, null);
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://clm.sonatype.com:8080/foo"));
    assertEquals("http://clm.sonatype.com:8080/dst", baseUrl.redirect().path("dst").build().toString());
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://clm.sonatype.com:8080/foo?x=y&a=b"));
    assertEquals("http://clm.sonatype.com:8080/dst/index.html?x=y&a=b", baseUrl.redirect().path("dst/index.html")
        .build().toString());
  }

  @Test
  public void testRedirect_XForwardedProtoHeaderSet() {
    InsightConfig appConfig = new InsightConfig();

    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com:8080")));

    HttpHeaders httpHeaders = mock(HttpHeaders.class);

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo, httpHeaders);

    when(httpHeaders.getRequestHeader(X_FORWARDED_PROTO)).thenReturn(Collections.singletonList("https"));
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://clm.sonatype.com:8080/foo?x=y&a=b"));
    assertEquals("https://clm.sonatype.com:8080/dst/index.html?x=y&a=b", baseUrl.redirect().path("dst/index.html")
        .build().toString());
  }

}
