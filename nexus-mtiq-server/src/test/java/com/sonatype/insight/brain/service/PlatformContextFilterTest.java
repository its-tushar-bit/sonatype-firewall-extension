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
import static org.mockito.Mockito.*;

public class PlatformContextFilterTest
    extends AbstractMultiTenantTest
{
  @Mock
  private FilterChain mockFilterChain;

  @Mock
  private HttpServletResponse mockHttpServletResponse;

  @Mock
  private RequestDispatcher mockRequestDispatcher;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private HttpServletRequest createMockRequest(String requestURI) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(requestURI);
    when(request.getRequestDispatcher(anyString())).thenReturn(mockRequestDispatcher);
    return request;
  }

  @Test
  public void testDoFilter_RootContextPath_PlatformAssetsIndex_Forwards() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/assets/index.html");

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify forward is called instead of continuing filter chain
    verify(mockRequestDispatcher).forward(request, mockHttpServletResponse);
    verify(mockFilterChain, never()).doFilter(any(), any());
  }

  @Test
  public void testDoFilter_RootContextPath_PlatformOidc_Rewrites() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/oidc/login");

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify filter chain is called with a wrapped request
    ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockHttpServletResponse));

    // Verify the wrapped request has the rewritten path
    ServletRequest wrappedRequest = requestCaptor.getValue();
    assertThat(wrappedRequest).isInstanceOf(HttpServletRequestWrapper.class);
    assertThat(((HttpServletRequest) wrappedRequest).getRequestURI()).isEqualTo("/oidc/login");
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
  public void testDoFilter_RootContextPath_PathRewriting() throws Exception {
    PlatformContextFilter filter = new PlatformContextFilter("/");
    HttpServletRequest request = createMockRequest("/platform/api/v2/applications");

    filter.doFilter(request, mockHttpServletResponse, mockFilterChain);

    // Verify filter chain is called with wrapped request
    ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
    verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockHttpServletResponse));

    // Verify the wrapped request has the rewritten path
    ServletRequest wrappedRequest = requestCaptor.getValue();
    assertThat(wrappedRequest).isInstanceOf(HttpServletRequestWrapper.class);
    assertThat(((HttpServletRequest) wrappedRequest).getRequestURI()).isEqualTo("/api/v2/applications");
  }
}
