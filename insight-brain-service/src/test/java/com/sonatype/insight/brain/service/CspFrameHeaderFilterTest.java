/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class CspFrameHeaderFilterTest
    extends AbstractBrainServiceTest
{
  private static final String ALLOWLIST = "frameAncestorsAllowlist";

  @Mock
  private ApiConfigurationService apiConfigurationService;

  @Mock
  private FilterChain chain;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private CspFrameHeaderFilter serverHeaderFilter;

  @Before
  public void setUp() throws Exception {
    openMocks(this);
    serverHeaderFilter = new CspFrameHeaderFilter(apiConfigurationService);
  }

  @Test
  public void testDoFilterDoesntAddCSFHeaderIfSelfIsNull() throws Exception {
    when(
        apiConfigurationService.getConfigurationNoAuthz(Collections.singleton(ALLOWLIST))).thenReturn(
        Collections.singletonMap(ALLOWLIST, null));
    serverHeaderFilter.doFilter(request, response, chain);
    verify(response, never()).addHeader(anyString(), anyString());
  }

  @Test
  public void testDoFilterDoesntAddCSFHeaderIfSelfIsEmpty() throws Exception {
    when(
        apiConfigurationService.getConfigurationNoAuthz(Collections.singleton(ALLOWLIST))).thenReturn(
        Collections.singletonMap(ALLOWLIST, Collections.emptyList()));
    serverHeaderFilter.doFilter(request, response, chain);
    verify(response, never()).addHeader(anyString(), anyString());
  }

  @Test
  public void testDoFilterAddsCSFHeaderForSeveralURLs() throws Exception {
    List<String> result = new ArrayList<>();
    result.add("some");
    result.add("newOne");
    when(
        apiConfigurationService.getConfigurationNoAuthz(Collections.singleton(ALLOWLIST))).thenReturn(
        Collections.singletonMap(ALLOWLIST, result));
    serverHeaderFilter.doFilter(request, response, chain);
    verify(response).addHeader("Content-Security-Policy", "frame-ancestors 'self' some newOne;");
  }

  @Test
  public void testDoFilterAddsCSFHeaderForSingleURL() throws Exception {
    when(
        apiConfigurationService.getConfigurationNoAuthz(Collections.singleton(ALLOWLIST))).thenReturn(
        Collections.singletonMap(ALLOWLIST, Collections.singletonList("some")));
    serverHeaderFilter.doFilter(request, response, chain);
    verify(response).addHeader("Content-Security-Policy", "frame-ancestors 'self' some;");
  }
}
