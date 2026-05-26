/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
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

  @Before
  public void before() {
    // Always start with baseUrl unconfigured
    resetBaseUrl();
    // Sanity check
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> baseUrl.getConfigured())
        .withMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  @After
  public void after() {
    baseUrl.release();
  }

  @Test
  public void testGet_OutsideHttpRequest_BaseUrlNotConfigured() {
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> baseUrl.get())
        .withMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  @Test
  public void testGet_UsesBaseUriFromHttpRequest() {
    setBaseUrl("http://localhost:8070");
    baseUrl.capture(httpRequest);

    mockHttpRequest("http://clm.sonatype.com:8080", "", "", "");
    assertThat(baseUrl.get()).isEqualTo("http://clm.sonatype.com:8080/");

    mockHttpRequest("http://clm.sonatype.com/", "", "", "");
    assertThat(baseUrl.get()).isEqualTo("http://clm.sonatype.com/");

    mockHttpRequest("http://clm.sonatype.com/", "/contextRoot", "/", "");
    assertThat(baseUrl.get()).isEqualTo("http://clm.sonatype.com/contextRoot/");
  }

  @Test
  public void testGet_UsesInsightConfigBaseUrl() {
    setBaseUrl("http://test.sonatype.com");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");

    setBaseUrl("http://test.sonatype.com/");
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");
  }

  @Test
  public void testGet_ForceInsightConfigBaseUrl() {
    baseUrl.capture(httpRequest);

    setBaseUrl("http://test.sonatype.com", true);
    assertThat(baseUrl.get()).isEqualTo("http://test.sonatype.com/");

    setBaseUrl("http://test.sonatype.com/", true);
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

    mockHttpRequest("http://clm.sonatype.com:8080", "", "/foo", "");
    assertThat(baseUrl.redirect().path(AssetPaths.BRAIN_ASSET_PATH).path("index.html").build().toString())
        .isEqualTo("http://clm.sonatype.com:8080/assets/index.html");
  }

  @Test
  public void testRedirect_EscapeCurlyBraces() {
    baseUrl.capture(httpRequest);

    mockHttpRequest("http://clm.sonatype.com:8080", "", "/", "key={%22hash%22:%22fe6e7a32c1228884b969%22}");
    assertThat(baseUrl.redirect().path("dst").build().toString()).isEqualTo(
        "http://clm.sonatype.com:8080/dst?key=%7B%22hash%22%3A%22fe6e7a32c1228884b969%22%7D");
  }

  @Test
  public void testGetConfigured_BaseUrlConfigured() {
    String configuredBaseUrl = "http://testBaseUrl:8070/";
    setBaseUrl(configuredBaseUrl);

    assertThat(baseUrl.getConfigured()).isEqualTo(configuredBaseUrl);
  }

  @Test
  public void testGetConfigured_BaseUrlNotConfigured() {
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> baseUrl.getConfigured())
        .withMessage(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  @Test
  public void testGet_ForwardedProtoHttp() {
    baseUrl.capture(httpRequest);
    String host = "test.sonatype.com";

    setBaseUrl("https://" + host, false);
    when(httpRequest.getHeader("x-forwarded-proto")).thenReturn("http");
    when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("https://" + host));
    when(httpRequest.getRequestURI()).thenReturn("");
    when(httpRequest.getContextPath()).thenReturn("");

    assertThat(baseUrl.get()).isEqualTo("http://" + host + "/");
  }

  @Test
  public void testGet_ForwardedProtoHttps() {
    baseUrl.capture(httpRequest);
    String host = "test.sonatype.com";

    setBaseUrl("http://" + host, false);
    when(httpRequest.getHeader("x-forwarded-proto")).thenReturn("https");
    when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://" + host));
    when(httpRequest.getRequestURI()).thenReturn("");
    when(httpRequest.getContextPath()).thenReturn("");

    assertThat(baseUrl.get()).isEqualTo("https://" + host + "/");
  }

  @Test
  public void testGet_ForwardedProtoInvalid_ShouldUseScheme() {
    baseUrl.capture(httpRequest);
    String host = "test.sonatype.com";

    setBaseUrl("http://" + host, false);
    when(httpRequest.getHeader("x-forwarded-proto")).thenReturn("INVALID");
    when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://" + host));
    when(httpRequest.getRequestURI()).thenReturn("");
    when(httpRequest.getContextPath()).thenReturn("");
    when(httpRequest.getScheme()).thenReturn("https");

    assertThat(baseUrl.get()).isEqualTo("https://" + host + "/");
  }
}
