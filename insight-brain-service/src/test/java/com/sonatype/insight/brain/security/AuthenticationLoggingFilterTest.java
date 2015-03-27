/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
    assertThat(MDC.get(MDCUsernameScope.USERNAME), is(nullValue()));
  }

  @After
  public void cleanup() {
    MDC.remove(MDCUsernameScope.USERNAME);
    assertThat(MDC.get(MDCUsernameScope.USERNAME), is(nullValue()));
  }

  @Test
  public void addsUsernameToMDC() throws IOException, ServletException {
    final String username = "foo";

    prepareMocks(username);
    FilterChainStub chain = new FilterChainStub();

    filter.doFilter(request, response, chain);

    assertThat(chain.mdcUsername, is(username));
  }

  @Test
  public void anonymousToMDC() throws IOException, ServletException {
    prepareMocks(null);
    FilterChainStub chain = new FilterChainStub();

    filter.doFilter(request, response, chain);

    assertThat(chain.mdcUsername, is(MDCUsernameScope.ANONYMOUS));
  }

  @Test
  public void mdcCleanup() throws IOException, ServletException {
    final String username = "foo";
    prepareMocks(username);

    filter.doFilter(request, response, mock(FilterChain.class));

    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    assertThat(contextMap == null || contextMap.isEmpty(), is(true));
  }

  private void prepareMocks(String username) {
    if (!StringUtils.isBlank(username)) {
      when(currentUser.getUsername()).thenReturn(username);
    } else {
      when(currentUser.isAnonymous()).thenReturn(true);
    }
    filter = new AuthenticationLoggingFilter(currentUser);
  }

  private class FilterChainStub
      implements FilterChain
  {
    private String mdcUsername;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response)
        throws IOException, ServletException
    {
      mdcUsername = MDC.get(MDCUsernameScope.USERNAME);
    }
  }
}
