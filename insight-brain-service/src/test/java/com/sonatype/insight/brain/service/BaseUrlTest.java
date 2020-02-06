/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class BaseUrlTest
    extends AbstractComponentTest
{
  @Inject
  private BaseUrl baseUrl;

  @Inject
  private InsightConfig appConfig;

  @Mock(lenient = true)
  private HttpServletRequest httpRequest;

  private void mockHttpRequest(String host, String contextPath, String servletPath, String queryParams) {
    if (host.endsWith("/") && !(contextPath.isEmpty() && servletPath.isEmpty())) {
      host = host.substring(0, host.length() - 1);
    }
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = '/' + contextPath;
    }
    if (!servletPath.isEmpty() && !servletPath.startsWith("/")) {
      servletPath = '/' + servletPath;
    }
    when(httpRequest.getRequestURL()).thenReturn(new StringBuffer(host + contextPath + servletPath));
    when(httpRequest.getRequestURI()).thenReturn(contextPath + servletPath);
    when(httpRequest.getContextPath()).thenReturn(contextPath);
    when(httpRequest.getQueryString()).thenReturn(queryParams);
  }

  @After
  public void exit() {
    baseUrl.release();
  }

  @Test
  public void testGet_OutsideHttpRequest_BaseUrlNotConfigured() throws Exception {
    appConfig.setBaseUrl(null);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      baseUrl.get();
    }).withMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  @Test
  public void testGet_UsesBaseUriFromHttpRequest() {
    appConfig.setBaseUrl("http://localhost:8070");
    baseUrl.capture(httpRequest);

    mockHttpRequest("http://clm.sonatype.com:8080", "", "", "");
    assertThat(baseUrl.get()).isEqualTo("http://clm.sonatype.com:8080/");

    mockHttpRequest("http://clm.sonatype.com/", "", "", "");
    assertThat(baseUrl.get()).isEqualTo("http://clm.sonatype.com/");

    mockHttpRequest("http://clm.sonatype.com/", "/contextRoot", "/", "");
    assertThat(baseUrl.get()).isEqualTo("http://clm.sonatype.com/contextRoot/");
  }

  @Test
  public void testGet_UsesInsightConfigBaseUrl() throws Exception {
    appConfig.setBaseUrl("http://test.sonatype.com");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");

    appConfig.setBaseUrl("http://test.sonatype.com/");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");
  }

  @Test
  public void testGet_ForceInsightConfigBaseUrl() throws Exception {
    appConfig.setForceBaseUrl(true);
    baseUrl.capture(httpRequest);

    appConfig.setBaseUrl("http://test.sonatype.com");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");

    appConfig.setBaseUrl("http://test.sonatype.com/");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");

    verifyNoInteractions(httpRequest);
  }

  @Test
  public void testRedirect() {
    baseUrl.capture(httpRequest);

    mockHttpRequest("http://clm.sonatype.com:8080", "", "/foo", "");
    assertThat(baseUrl.redirect().path("dst").build().toString()).isEqualTo("http://clm.sonatype.com:8080/dst");

    mockHttpRequest("http://clm.sonatype.com:8080", "", "/foo", "x=y&a=b");
    assertThat(baseUrl.redirect().path("dst/index.html").build().toString())
        .isEqualTo("http://clm.sonatype.com:8080/dst/index.html?x=y&a=b");
  }

  @Test
  public void testGetConfigured_BaseUrlConfigured() {
    String configuredBaseUrl = "http://testBaseUrl:8070/";
    appConfig.setBaseUrl(configuredBaseUrl);

    assertThat(baseUrl.getConfigured()).isEqualTo(configuredBaseUrl);
  }

  @Test
  public void testGetConfigured_BaseUrlNotConfigured() {
    appConfig.setBaseUrl(null);
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
      baseUrl.getConfigured();
    }).withMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }
}
