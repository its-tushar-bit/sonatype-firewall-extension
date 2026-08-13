/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import jakarta.ws.rs.core.HttpHeaders;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class StaticAssetCacheControlFilterTest
{
  private final StaticAssetCacheControlFilter filter = new StaticAssetCacheControlFilter();

  private MockHttpServletResponse doFilter(String uri, String queryString) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setQueryString(queryString);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isSameAs(request);
    return response;
  }

  @Test
  public void testDoFilter_versionedAsset_setsLongLivedCacheControl() throws Exception {
    MockHttpServletResponse response = doFilter("/assets/bundle.js", "1786378686200");

    assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL))
        .isEqualTo(StaticAssetCacheControlFilter.CACHE_CONTROL_VALUE);
  }

  @Test
  public void testDoFilter_unversionedAsset_doesNotSetCacheControl() throws Exception {
    MockHttpServletResponse response = doFilter("/assets/fonts/sonatype-icons.woff", null);

    assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isNull();
  }

  @Test
  public void testDoFilter_emptyQueryString_doesNotSetCacheControl() throws Exception {
    MockHttpServletResponse response = doFilter("/assets/fonts/fontawesome-webfont.eot", "");

    assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isNull();
  }

  @Test
  public void testDoFilter_versionedIndexPage_doesNotSetCacheControl() throws Exception {
    MockHttpServletResponse response = doFilter("/assets/index.html", "1786378686200");

    assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isNull();
  }

  @Test
  public void testDoFilter_mixedCaseHtml_doesNotSetCacheControl() throws Exception {
    MockHttpServletResponse response = doFilter("/assets/Index.HTML", "1786378686200");

    assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).isNull();
  }
}
