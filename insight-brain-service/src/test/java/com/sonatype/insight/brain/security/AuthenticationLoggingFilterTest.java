/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.ee11.servlet.ServletApiRequest;
import org.eclipse.jetty.server.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationLoggingFilterTest
{
  private final HttpServletRequest request = mock(HttpServletRequest.class);

  private final HttpServletResponse response = mock(HttpServletResponse.class);

  private final CurrentUser currentUser = mock(CurrentUser.class);

  private AuthenticationLoggingFilter filter;

  /**
   * JUnit reuses threads which may have MDC username set as {@link MDCUsernameScope.SYSTEM }
   */
  @Before
  public void setup() {
    MDC.remove(MDCUsernameScope.USERNAME);
    assertThat(MDC.get(MDCUsernameScope.USERNAME)).isNull();
  }

  @After
  public void cleanup() {
    MDC.remove(MDCUsernameScope.USERNAME);
    assertThat(MDC.get(MDCUsernameScope.USERNAME)).isNull();
  }

  @Test
  public void testAuthenticatedUser() throws IOException, ServletException {
    final String username = "foo";

    prepareMocks(username);
    FilterChainStub chain = new FilterChainStub();

    filter.doFilter(request, response, chain);

    assertThat(chain.mdcUsername).isEqualTo(username);
  }

  @Test
  public void testAnonymous() throws IOException, ServletException {
    prepareMocks(null);
    FilterChainStub chain = new FilterChainStub();

    filter.doFilter(request, response, chain);

    assertThat(chain.mdcUsername).isEqualTo(MDCUsernameScope.ANONYMOUS);
  }

  @Test
  public void testMDCIsCleanAfterRequestIsProcessed() throws IOException, ServletException {
    final String username = "foo";
    prepareMocks(username);

    filter.doFilter(request, response, mock(FilterChain.class));

    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    assertThat(contextMap).isNullOrEmpty();
  }

  /**
   * Tests that authenticated username is correctly set on the Jetty Request's AuthenticationState
   * after unwrapping Shiro's ServletRequestWrapper.
   * <p>
   * Note: We capture the AuthenticationState via setAttribute mock behavior rather than using
   * Request.getAuthenticationState() because the latter is a static method that reads from
   * a real request attribute, which doesn't work with mock Request objects. Capturing via
   * doAnswer is the most practical way to verify the correct AuthenticationState is set
   * without requiring a full Jetty server in the test.
   * <p>
   * The unit tests in this class provide comprehensive coverage of the unwrapping logic and
   * AuthenticationState setting. An end-to-end integration test with actual HTTP requests
   * would require significant test infrastructure setup and is deferred to future work.
   */
  @Test
  public void testRequestLoggingUsernameIsSetOnJettyRequest() throws IOException, ServletException {
    final String username = "testuser";
    prepareMocks(username);

    // Create mock Jetty Request and ServletApiRequest
    Request mockJettyRequest = mock(Request.class);
    ServletApiRequest mockServletApiRequest = mock(ServletApiRequest.class);
    when(mockServletApiRequest.getRequest()).thenReturn(mockJettyRequest);

    // Wrap in ServletRequestWrapper to simulate Shiro's wrapping
    ServletRequestWrapper wrappedRequest = new ServletRequestWrapper(mockServletApiRequest);

    // Capture the AuthenticationState that gets set via setAttribute
    final Request.AuthenticationState[] capturedAuthState = new Request.AuthenticationState[1];
    org.mockito.Mockito.doAnswer(invocation -> {
      capturedAuthState[0] = invocation.getArgument(1);
      return null;
    })
        .when(mockJettyRequest)
        .setAttribute(
            org.mockito.ArgumentMatchers.eq(Request.AuthenticationState.class.getName()),
            org.mockito.ArgumentMatchers.any());

    FilterChain chain = mock(FilterChain.class);
    filter.doFilter(wrappedRequest, response, chain);

    // Verify AuthenticationState was set with correct username
    assertThat(capturedAuthState[0]).isNotNull();
    Principal principal = capturedAuthState[0].getUserPrincipal();
    assertThat(principal).isNotNull();
    assertThat(principal.getName()).isEqualTo(username);
  }

  @Test
  public void testRequestLoggingUsernameNotSetForAnonymous() throws IOException, ServletException {
    prepareMocks(null); // Anonymous user

    Request mockJettyRequest = mock(Request.class);
    ServletApiRequest mockServletApiRequest = mock(ServletApiRequest.class);
    when(mockServletApiRequest.getRequest()).thenReturn(mockJettyRequest);

    ServletRequestWrapper wrappedRequest = new ServletRequestWrapper(mockServletApiRequest);

    FilterChain chain = mock(FilterChain.class);
    filter.doFilter(wrappedRequest, response, chain);

    // Verify Request.setAuthenticationState was never called for anonymous users
    org.mockito.Mockito.verify(mockJettyRequest, org.mockito.Mockito.never())
        .setAttribute(
            org.mockito.ArgumentMatchers.eq(Request.AuthenticationState.class.getName()),
            org.mockito.ArgumentMatchers.any());
  }

  private void prepareMocks(String username) {
    if (!StringUtils.isBlank(username)) {
      when(currentUser.getUsername()).thenReturn(username);
    }
    else {
      when(currentUser.isAnonymous()).thenReturn(true);
    }
    filter = new AuthenticationLoggingFilter(currentUser);
  }

  private static class FilterChainStub
      implements FilterChain
  {
    private String mdcUsername;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response) {
      mdcUsername = MDC.get(MDCUsernameScope.USERNAME);
    }
  }
}
