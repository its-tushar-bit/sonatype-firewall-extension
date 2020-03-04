/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
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
  private final Request jettyRequest = new Request(null, null);

  private ServletRequest request = new ServletRequestWrapper(jettyRequest);

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
    assertJettyRequestAuthentication(username);
  }

  @Test
  public void testAnonymous() throws IOException, ServletException {
    prepareMocks(null);
    FilterChainStub chain = new FilterChainStub();

    filter.doFilter(request, response, chain);

    assertThat(chain.mdcUsername).isEqualTo(MDCUsernameScope.ANONYMOUS);
    assertJettyRequestAuthentication(null);
  }

  @Test
  public void testMDCIsCleanAfterRequestIsProcessed() throws IOException, ServletException {
    final String username = "foo";
    prepareMocks(username);

    filter.doFilter(request, response, mock(FilterChain.class));

    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    assertThat(contextMap).isNullOrEmpty();
  }

  @Test
  public void testMultipleLevelsOfRequestWrapping() throws IOException, ServletException {
    final String username = "foo";

    prepareMocks(username);
    FilterChainStub chain = new FilterChainStub();

    // e.g. com.yammer.dropwizard.jetty.BiDiGzipHandler.GzipServletRequest
    request = new ServletRequestWrapper(new ServletRequestWrapper(request));

    filter.doFilter(request, response, chain);

    assertThat(chain.mdcUsername).isEqualTo(username);
    assertJettyRequestAuthentication(username);
  }

  private void assertJettyRequestAuthentication(String username) {
    Authentication authentication = jettyRequest.getAuthentication();
    if (username == null) {
      assertThat(authentication).isNull();
    }
    else {
      assertThat(authentication).isInstanceOf(UserAuthentication.class);
      assertThat(((UserAuthentication) authentication).getUserIdentity().getUserPrincipal().getName())
          .isEqualTo(username);
    }
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

  private class FilterChainStub
      implements FilterChain
  {
    private String mdcUsername;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response) throws IOException,
        ServletException
    {
      mdcUsername = MDC.get(MDCUsernameScope.USERNAME);
    }
  }
}
