/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.CspFrameHeaderFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * H2 port of {@code CspFrameHeaderFilterTest}. Pure Mockito unit test — the injected {@link IqTestContext} is
 * unused because the class under test does not touch the running server.
 */
@IqH2Test
class IqH2CspFrameHeaderFilterTest
{
  private IqTestContext ctx;

  @Mock
  private Configuration configuration;

  @Mock
  private FilterChain chain;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  private CspFrameHeaderFilter serverHeaderFilter;

  @BeforeEach
  void setUp() {
    openMocks(this);
    serverHeaderFilter = new CspFrameHeaderFilter(configuration);
  }

  @Test
  void testDoFilterDoesntAddCSFHeaderIfSelfIsNull() throws Exception {
    when(configuration.getFrameAncestorsAllowList()).thenReturn(null);
    serverHeaderFilter.doFilter(request, response, chain);
    verify(response, never()).addHeader(anyString(), anyString());
  }

  @Test
  void testDoFilterDoesntAddCSFHeaderIfSelfIsEmpty() throws Exception {
    when(configuration.getFrameAncestorsAllowList()).thenReturn(Collections.emptyList());
    serverHeaderFilter.doFilter(request, response, chain);
    verify(response, never()).addHeader(anyString(), anyString());
  }

  @Test
  void testDoFilterAddsCSFHeaderForSeveralURLs() throws Exception {
    List<String> result = new ArrayList<>();
    result.add("'self'");
    result.add("some");
    result.add("newOne");
    when(configuration.getFrameAncestorsAllowList()).thenReturn(result);
    serverHeaderFilter.doFilter(request, response, chain);
    verify(response).addHeader("Content-Security-Policy", "frame-ancestors 'self' some newOne;");
  }

  @Test
  void testDoFilterAddsCSFHeaderForSingleURL() throws Exception {
    List<String> result = new ArrayList<>();
    result.add("'self'");
    when(configuration.getFrameAncestorsAllowList()).thenReturn(result);
    serverHeaderFilter.doFilter(request, response, chain);
    verify(response).addHeader("Content-Security-Policy", "frame-ancestors 'self';");
  }

  @Test
  void testDoFilterLeavesJustSingleSelfInAllowedList() throws Exception {
    List<String> allowList = new ArrayList<>();
    allowList.add("'self'");
    allowList.add("some");
    allowList.add("newOne");
    when(configuration.getFrameAncestorsAllowList()).thenReturn(allowList);
    serverHeaderFilter.doFilter(request, response, chain);
    verify(response).addHeader("Content-Security-Policy", "frame-ancestors 'self' some newOne;");
  }
}
