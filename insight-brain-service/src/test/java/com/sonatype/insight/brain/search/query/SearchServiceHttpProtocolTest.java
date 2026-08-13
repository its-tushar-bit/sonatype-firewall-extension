/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.search.export.LifecycleSearchRowFactory;
import com.sonatype.insight.brain.search.export.SbomSearchRowFactory;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for HTTP protocol version handling in SearchService export functionality.
 * Verifies that trailers are only set for HTTP/1.1 and later, not for HTTP/1.0.
 */
@ExtendWith(MockitoExtension.class)
public class SearchServiceHttpProtocolTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private LifecycleSearchRowFactory lifecycleSearchRowFactory;

  @Mock
  private SbomSearchRowFactory sbomSearchRowFactory;

  @Mock
  private Configuration configuration;

  @Mock
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private SearchService searchService;

  @BeforeEach
  public void setUp() {
    searchService = new SearchService(
        searchIndexClient,
        lifecycleSearchRowFactory,
        sbomSearchRowFactory,
        configuration,
        systemConfigurationPropertyDAO);
  }

  @Test
  public void testSupportsTrailers_Http10_ReturnsFalse() {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getProtocol()).thenReturn("HTTP/1.0");

    boolean result = searchService.supportsTrailers(mockRequest);

    verify(mockRequest).getProtocol();
    assertThat(result).isFalse();
  }

  @Test
  public void testSupportsTrailers_Http11_ReturnsTrue() {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getProtocol()).thenReturn("HTTP/1.1");

    boolean result = searchService.supportsTrailers(mockRequest);

    verify(mockRequest).getProtocol();
    assertThat(result).isTrue();
  }

  @Test
  public void testSupportsTrailers_Http2_ReturnsTrue() {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getProtocol()).thenReturn("HTTP/2");

    boolean result = searchService.supportsTrailers(mockRequest);

    verify(mockRequest).getProtocol();
    assertThat(result).isTrue();
  }

  @Test
  public void testSupportsTrailers_NullProtocol_ReturnsFalse() {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getProtocol()).thenReturn(null);

    boolean result = searchService.supportsTrailers(mockRequest);

    verify(mockRequest).getProtocol();
    assertThat(result).isFalse();
  }
}
