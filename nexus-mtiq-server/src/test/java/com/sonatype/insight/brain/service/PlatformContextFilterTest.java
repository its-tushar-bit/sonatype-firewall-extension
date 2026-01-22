/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlatformContextFilterTest
    extends AbstractMultiTenantTest
{
  @Mock
  private FilterChain mockFilterChain;

  @Mock
  private HttpServletResponse mockHttpServletResponse;

  @Mock
  private RequestDispatcher mockRequestDispatcher;

  @Mock
  private jakarta.servlet.ServletContext mockServletContext;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    // Configure ServletContext to return the mockRequestDispatcher for any path
    when(mockServletContext.getRequestDispatcher(anyString())).thenReturn(mockRequestDispatcher);
  }

  private HttpServletRequest createMockRequest(final String requestURI) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(requestURI);
    when(request.getServletContext()).thenReturn(mockServletContext);
    when(request.getRequestDispatcher(anyString())).thenReturn(mockRequestDispatcher);
    return request;
  }

  @Test
  public void testDoFilter_RootContextPath_PlatformAssetsIndex_Forwards() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/assets/index.html");
    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("localhost");
    when(request.getServerPort()).thenReturn(8080);

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify that forward() is called, not the filter chain
    ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(mockRequestDispatcher).forward(requestCaptor.capture(), eq(mockHttpServletResponse));
    verify(mockFilterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));

    // Verify the wrapped request has the rewritten path split correctly
    ServletRequest wrappedRequest = requestCaptor.getValue();
    assertThat(wrappedRequest).isInstanceOf(HttpServletRequestWrapper.class);
    HttpServletRequest httpWrapped = (HttpServletRequest) wrappedRequest;
    assertThat(httpWrapped.getRequestURI()).isEqualTo("/assets/index.html");
    assertThat(httpWrapped.getServletPath()).isEqualTo("/assets");
    assertThat(httpWrapped.getPathInfo()).isEqualTo("/index.html");
    assertThat(httpWrapped.getContextPath()).isEqualTo("");
    // CRITICAL: getRequestURL() must preserve /platform for JavaScript BASE_URL detection
    assertThat(httpWrapped.getRequestURL().toString()).isEqualTo("http://localhost:8080/platform/assets/index.html");
  }

  @Test
  public void testDoFilter_RootContextPath_PlatformOidc_Rewrites() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/oidc/login");
    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("localhost");
    when(request.getServerPort()).thenReturn(8080);

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify filter chain is called with a wrapped request
    ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockHttpServletResponse));

    // Verify the wrapped request has the rewritten path
    ServletRequest wrappedRequest = requestCaptor.getValue();
    assertThat(wrappedRequest).isInstanceOf(HttpServletRequestWrapper.class);
    HttpServletRequest httpWrapped = (HttpServletRequest) wrappedRequest;
    assertThat(httpWrapped.getRequestURI()).isEqualTo("/oidc/login");
    assertThat(httpWrapped.getServletPath()).isEqualTo("");
    assertThat(httpWrapped.getPathInfo()).isEqualTo("/oidc/login");
    assertThat(httpWrapped.getContextPath()).isEqualTo("");
    // Verify getRequestURL() preserves /platform for consistency
    assertThat(httpWrapped.getRequestURL().toString()).isEqualTo("http://localhost:8080/platform/oidc/login");
  }

  @Test
  public void testDoFilter_PlatformContextPath_WithPlatformPrefix_Continues() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/platform/");
    HttpServletRequest request = createMockRequest("/platform/assets/index.html");

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify original request is passed through unchanged
    verify(mockFilterChain).doFilter(request, mockHttpServletResponse);
  }

  @Test
  public void testDoFilter_RootContextPath_PlatformRoot_RedirectsToPlatformIndex() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/");

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    verify(mockFilterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    verify(mockHttpServletResponse).sendRedirect("/platform/assets/index.html");
  }

  @Test
  public void testDoFilter_RootContextPath_Platform_RedirectsToPlatformIndex() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform");

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    verify(mockFilterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    verify(mockHttpServletResponse).sendRedirect("/platform/assets/index.html");
  }

  @Test
  public void testDoFilter_RootContextPath_PlatformRestProductVersion_Rewrites() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/rest/product/version");
    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("localhost");
    when(request.getServerPort()).thenReturn(8080);

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify filter chain is called with wrapped request
    ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockHttpServletResponse));

    // Verify the wrapped request has the rewritten path
    // This is critical: Jersey uses PathInfo for routing when mapped at /*
    // The filter must rewrite /platform/rest/product/version so Jersey can route it
    // and Shiro can apply anonymous access rules to /rest/product/version
    ServletRequest wrappedRequest = requestCaptor.getValue();
    assertThat(wrappedRequest).isInstanceOf(HttpServletRequestWrapper.class);
    HttpServletRequest httpWrapped = (HttpServletRequest) wrappedRequest;
    assertThat(httpWrapped.getRequestURI()).isEqualTo("/rest/product/version");
    assertThat(httpWrapped.getServletPath()).isEqualTo("");
    assertThat(httpWrapped.getPathInfo()).isEqualTo("/rest/product/version");
    assertThat(httpWrapped.getContextPath()).isEqualTo("");
    // Verify getRequestURL() preserves /platform for JavaScript BASE_URL detection
    assertThat(httpWrapped.getRequestURL().toString()).isEqualTo("http://localhost:8080/platform/rest/product/version");
  }

  @Test
  public void testDoFilter_RootContextPath_NoPlatformPrefix_PassesThrough() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/rest/product/version");

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify original request is passed through unchanged when path doesn't start with /platform
    verify(mockFilterChain).doFilter(request, mockHttpServletResponse);
    verify(mockHttpServletResponse, never()).sendRedirect(anyString());
  }

  @Test
  public void testDoFilter_RootContextPath_AssetServletNotFound_Returns404() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/assets/missing.js");
    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("localhost");
    when(request.getServerPort()).thenReturn(8080);

    // Configure both dispatchers to return null (asset servlet not found)
    when(mockServletContext.getRequestDispatcher(anyString())).thenReturn(null);
    when(mockServletContext.getNamedDispatcher("assets")).thenReturn(null);

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify 404 error is sent when asset servlet is not found
    verify(mockHttpServletResponse).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    verify(mockFilterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
  }

  @Test
  public void testDoFilter_RootContextPath_AssetServletFallbackToNamedDispatcher() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/assets/app.js");
    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("localhost");
    when(request.getServerPort()).thenReturn(8080);

    // Configure path dispatcher to return null, but named dispatcher works
    when(mockServletContext.getRequestDispatcher(anyString())).thenReturn(null);
    when(mockServletContext.getNamedDispatcher("assets")).thenReturn(mockRequestDispatcher);

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify forward is called via named dispatcher fallback
    verify(mockRequestDispatcher).forward(any(ServletRequest.class), eq(mockHttpServletResponse));
    verify(mockFilterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
  }

  @Test
  public void testDoFilter_RootContextPath_StandardHttpPort_OmitsPortInUrl() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/api/v2/test");
    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("example.com");
    when(request.getServerPort()).thenReturn(80);

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockHttpServletResponse));

    HttpServletRequest httpWrapped = (HttpServletRequest) requestCaptor.getValue();
    // Port 80 should be omitted for http
    assertThat(httpWrapped.getRequestURL().toString()).isEqualTo("http://example.com/platform/api/v2/test");
  }

  @Test
  public void testDoFilter_RootContextPath_StandardHttpsPort_OmitsPortInUrl() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/api/v2/test");
    when(request.getScheme()).thenReturn("https");
    when(request.getServerName()).thenReturn("example.com");
    when(request.getServerPort()).thenReturn(443);

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockHttpServletResponse));

    HttpServletRequest httpWrapped = (HttpServletRequest) requestCaptor.getValue();
    // Port 443 should be omitted for https
    assertThat(httpWrapped.getRequestURL().toString()).isEqualTo("https://example.com/platform/api/v2/test");
  }

  @Test
  public void testDoFilter_RootContextPath_HttpsWithCustomPort_IncludesPort() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/api/v2/test");
    when(request.getScheme()).thenReturn("https");
    when(request.getServerName()).thenReturn("example.com");
    when(request.getServerPort()).thenReturn(8443);

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockHttpServletResponse));

    HttpServletRequest httpWrapped = (HttpServletRequest) requestCaptor.getValue();
    // Custom port should be included
    assertThat(httpWrapped.getRequestURL().toString()).isEqualTo("https://example.com:8443/platform/api/v2/test");
  }

  @Test
  public void testDoFilter_RootContextPath_AssetsPathExactly_HandlesEdgeCase() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/assets");
    when(request.getScheme()).thenReturn("http");
    when(request.getServerName()).thenReturn("localhost");
    when(request.getServerPort()).thenReturn(8080);

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify forward is called for /assets path
    ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(mockRequestDispatcher).forward(requestCaptor.capture(), eq(mockHttpServletResponse));

    HttpServletRequest httpWrapped = (HttpServletRequest) requestCaptor.getValue();
    assertThat(httpWrapped.getRequestURI()).isEqualTo("/assets");
    assertThat(httpWrapped.getServletPath()).isEqualTo("/assets");
    // Edge case: /assets exactly has no path info (length == 7, not > 7)
    assertThat(httpWrapped.getPathInfo()).isEqualTo("/assets");
  }
}
