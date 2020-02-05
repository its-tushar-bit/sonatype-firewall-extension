/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      baseUrl.get();
    }).withMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  @Test
  public void testGet_UsesBaseUri() {
    InsightConfig appConfig = new InsightConfig();
    appConfig.setBaseUrl("http://localhost:8070");

    UriInfo uriInfo = mock(UriInfo.class);

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo);
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com:8080")));
    assertThat(baseUrl.get()).isEqualTo("http://clm.sonatype.com:8080/");
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com/")));
    assertThat(baseUrl.get()).isEqualTo("http://clm.sonatype.com/");
    when(uriInfo.getBaseUriBuilder())
        .thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com/contextRoot/")));
    assertThat(baseUrl.get()).isEqualTo("http://clm.sonatype.com/contextRoot/");
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

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo);
    appConfig.setBaseUrl("http://test.sonatype.com");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");
    appConfig.setBaseUrl("http://test.sonatype.com/");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");
  }

  @Test
  public void testGet_ForceInsightConfigBaseUrl() throws Exception {
    testGet_ForceInsightConfigBaseUrl(null);

    UriInfo uriInfo = mock(UriInfo.class);
    testGet_ForceInsightConfigBaseUrl(uriInfo);
    verifyNoInteractions(uriInfo);
  }

  private void testGet_ForceInsightConfigBaseUrl(UriInfo uriInfo) {
    InsightConfig appConfig = new InsightConfig();
    appConfig.setForceBaseUrl(true);

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo);
    appConfig.setBaseUrl("http://test.sonatype.com");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");
    appConfig.setBaseUrl("http://test.sonatype.com/");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");
  }

  @Test
  public void testRedirect() {
    InsightConfig appConfig = new InsightConfig();

    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUriBuilder()).thenReturn(UriBuilder.fromUri(URI.create("http://clm.sonatype.com:8080")));

    BaseUrl baseUrl = new BaseUrl(appConfig, uriInfo);
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://clm.sonatype.com:8080/foo"));
    assertThat(baseUrl.redirect().path("dst").build().toString()).isEqualTo("http://clm.sonatype.com:8080/dst");
    when(uriInfo.getRequestUri()).thenReturn(URI.create("http://clm.sonatype.com:8080/foo?x=y&a=b"));
    assertThat(baseUrl.redirect().path("dst/index.html").build().toString())
        .isEqualTo("http://clm.sonatype.com:8080/dst/index.html?x=y&a=b");
  }

  @Test
  public void testGetConfigured_BaseUrlConfigured() {
    String configuredBaseUrl = "http://testBaseUrl:8070/";
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setBaseUrl(configuredBaseUrl);

    BaseUrl baseUrl = new BaseUrl(insightConfig, null);
    assertThat(baseUrl.getConfigured()).isEqualTo(configuredBaseUrl);
  }

  @Test
  public void testGetConfigured_BaseUrlNotConfigured() {
    InsightConfig insightConfig = new InsightConfig();
    BaseUrl baseUrl = new BaseUrl(insightConfig, null);

    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      baseUrl.getConfigured();
    }).withMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }
}
