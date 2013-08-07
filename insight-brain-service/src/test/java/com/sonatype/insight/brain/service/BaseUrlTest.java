/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import org.junit.Test;

public class BaseUrlTest
{

  @Test
  public void testGet_BaseUrlNotConfigured() {
    InsightConfig appConfig = new InsightConfig();

    UriInfo uriInfo = mock(UriInfo.class);

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://clm.sonatype.com:8080"));
    assertEquals("http://clm.sonatype.com:8080/", baseUrl.get());
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://clm.sonatype.com/"));
    assertEquals("http://clm.sonatype.com/", baseUrl.get());
  }

  @Test
  public void testGet_BaseUrlConfigured() {
    InsightConfig appConfig = new InsightConfig();

    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://clm.sonatype.com:8080"));

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo);
    appConfig.setBaseUrl("http://test.sonatype.com");
    assertEquals("http://test.sonatype.com/", baseUrl.get());
    appConfig.setBaseUrl("http://test.sonatype.com/");
    assertEquals("http://test.sonatype.com/", baseUrl.get());
  }

  @Test
  public void testRedirect() {
    InsightConfig appConfig = new InsightConfig();

    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://clm.sonatype.com:8080"));

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo);
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://clm.sonatype.com:8080/foo"));
    assertEquals("http://clm.sonatype.com:8080/dst", baseUrl.redirect().path("dst").build().toString());
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://clm.sonatype.com:8080/foo?x=y&a=b"));
    assertEquals("http://clm.sonatype.com:8080/dst/index.html?x=y&a=b", baseUrl.redirect().path("dst/index.html")
        .build().toString());
  }

}
