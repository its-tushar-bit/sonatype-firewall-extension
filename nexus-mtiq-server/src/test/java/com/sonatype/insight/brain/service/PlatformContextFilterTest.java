/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.pathmap.MatchedPath;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.ServletPathMapping;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class PlatformContextFilterTest
    extends AbstractMultiTenantTest
{
  private static final HttpURI platformIndexUri = HttpURI.from("GET", "/platform/assets/index.html");

  private static final HttpURI platformRootUri = HttpURI.from("GET", "/platform/");

  private static final HttpURI platformUri = HttpURI.from("GET", "/platform");

  private static final HttpURI platformOidcUri = HttpURI.from("GET", "/platform/oidc/login");

  @Mock
  private FilterChain mockFilterChain;

  @Mock
  private HttpChannel mockHttpChannel;

  @Mock
  private HttpServletResponse mockHttpServletResponse;

  @Mock
  private RequestDispatcher mockRequestDispatcher;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private Request createMockRequest(HttpURI httpURI) {
    Request request = mock(Request.class);
    when(request.getHttpURI()).thenReturn(httpURI);
    when(request.getHttpChannel()).thenReturn(mockHttpChannel);
    // Mock ServletPathMapping with a default "/*" pattern
    PathSpec pathSpec = PathSpec.from("/*");
    MatchedPath matchedPath = pathSpec.matched(httpURI.getPath());
    ServletPathMapping servletPathMapping = new ServletPathMapping(pathSpec, "default", httpURI.getPath(), matchedPath);
    when(request.getServletPathMapping()).thenReturn(servletPathMapping);
    // Mock other path-related methods
    when(request.getPathInContext()).thenReturn(httpURI.getPath());
    when(request.getRequestURI()).thenReturn(httpURI.getPath());
    when(request.getServletPath()).thenReturn("");
    when(request.getPathInfo()).thenReturn(httpURI.getPath());
    when(request.getOriginalURI()).thenReturn(httpURI.toString());
    return request;
  }

  private Response createMockResponse() {
    Response response = mock(Response.class);
    when(response.getHttpChannel()).thenReturn(mockHttpChannel);
    return response;
  }

  @Test
  public void testDoFilter_RootContextPath_PlatformAssetsIndex_Forwards() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    Request request = createMockRequest(platformIndexUri);
    Response response = createMockResponse();

    // Mock RequestDispatcher for forwarding
    when(request.getRequestDispatcher("/assets/index.html")).thenReturn(mockRequestDispatcher);

    filter.doFilter(request, response, mockFilterChain);

    // Verify forward is called instead of continuing filter chain
    verify(mockRequestDispatcher).forward(request, response);
    verify(mockFilterChain, never()).doFilter(any(), any());
  }

  @Test
  public void testDoFilter_RootContextPath_PlatformOidc_Rewrites() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    Request request = createMockRequest(platformOidcUri);
    Response response = createMockResponse();
    filter.doFilter(request, response, mockFilterChain);
    verify(mockFilterChain).doFilter(request, response);
    // Verify that setHttpURI was called with the rewritten path
    verify(request).setHttpURI(argThat(uri -> uri.getPath().equals("/oidc/login")));
  }

  @Test
  public void testDoFilter_PlatformContextPath_WithPlatformPrefix_Continues() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/platform/");
    Request request = createMockRequest(platformIndexUri);
    Response response = createMockResponse();
    filter.doFilter(request, response, mockFilterChain);
    verify(mockFilterChain).doFilter(request, response);
    verify(request, never()).setHttpURI(any());
  }

  @Test
  public void testDoFilter_RootContextPath_PlatformRoot_RedirectsToPlatformIndex() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    Request request = createMockRequest(platformRootUri);
    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);
    verify(mockFilterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    verify(mockHttpServletResponse).sendRedirect("/platform/assets/index.html");
  }

  @Test
  public void testDoFilter_RootContextPath_Platform_RedirectsToPlatformIndex() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    Request request = createMockRequest(platformUri);
    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);
    verify(mockFilterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    verify(mockHttpServletResponse).sendRedirect("/platform/assets/index.html");
  }

  @Test
  public void testDoFilter_RootContextPath_PreservesQuery() throws Exception {
    HttpURI apiUriWithQuery = HttpURI.from("GET", "/platform/api/v2/applications?page=1&size=10");
    PlatformContextFilter filter = new PlatformContextFilter("/");
    Request request = createMockRequest(apiUriWithQuery);
    Response response = createMockResponse();
    filter.doFilter(request, response, mockFilterChain);
    verify(mockFilterChain).doFilter(request, response);
    // Verify that setHttpURI was called and the query is preserved
    verify(request).setHttpURI(argThat(uri ->
        uri.getPath().equals("/api/v2/applications") && uri.getQuery().equals("page=1&size=10")
    ));
  }
}
