/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.firewall;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class FirewallRedirectFilterTest
    extends AbstractBrainServiceIntegrationTest
{
  @InjectMocks
  private FirewallRedirectFilter firewallRedirectFilter;

  @Mock
  private HttpServletRequest mockHttpServletRequest;

  @Mock
  private HttpServletResponse mockHttpServletResponse;

  @Mock
  private FilterChain mockFilterChain;

  @Mock
  private RequestDispatcher mockRequestDispatcher;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testDoFilter_RedirectsDeprecatedMalwareDefensePath() throws Exception {
    when(mockHttpServletRequest.getPathInfo()).thenReturn("/api/v2/malware-defense/somePath");
    when(mockHttpServletRequest.getRequestDispatcher(anyString())).thenReturn(mockRequestDispatcher);

    firewallRedirectFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockHttpServletRequest).getRequestDispatcher("/" + PublicApiPaths.FIREWALL_RESOURCE_PATH + "/somePath");
    verify(mockRequestDispatcher).forward(mockHttpServletRequest, mockHttpServletResponse);
    verify(mockFilterChain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
  }

  @Test
  public void testDoFilter_AllowsNonDeprecatedFirewallPath() throws Exception {
    when(mockHttpServletRequest.getPathInfo()).thenReturn("/api/v2/notMalwareDefense/somePath");

    firewallRedirectFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

    verify(mockHttpServletResponse, never()).sendRedirect(anyString());
    verify(mockFilterChain).doFilter(mockHttpServletRequest, mockHttpServletResponse);
  }
}
