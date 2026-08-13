/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.HstsConfig;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class HstsHeaderFilterTest
{
  @Test
  public void shouldAddHstsHeaderOnSecureRequest() throws Exception {
    InsightConfig config = new InsightConfig();
    config.getHstsConfig().setEnabled(true);

    HstsHeaderFilter filter = new HstsHeaderFilter(config);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(true);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("Strict-Transport-Security"))
        .isEqualTo("max-age=" + (365L * 24 * 60 * 60) + "; includeSubDomains");
    Mockito.verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldNotAddHstsHeaderOnPlainHttpRequest() throws Exception {
    InsightConfig config = new InsightConfig();
    config.getHstsConfig().setEnabled(true);

    HstsHeaderFilter filter = new HstsHeaderFilter(config);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(false);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    Mockito.verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldNotAddHstsHeaderWhenDisabled() throws Exception {
    InsightConfig config = new InsightConfig();
    config.getHstsConfig().setEnabled(false);

    HstsHeaderFilter filter = new HstsHeaderFilter(config);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(true);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    Mockito.verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldIncludePreloadDirectiveWhenEnabled() throws Exception {
    InsightConfig config = new InsightConfig();
    HstsConfig hstsConfig = config.getHstsConfig();
    hstsConfig.setEnabled(true);
    hstsConfig.setPreload(true);
    hstsConfig.setMaxAgeSeconds(86400);

    HstsHeaderFilter filter = new HstsHeaderFilter(config);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(true);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("Strict-Transport-Security"))
        .isEqualTo("max-age=86400; includeSubDomains; preload");
  }

  @Test
  public void shouldOmitIncludeSubDomainsWhenDisabled() throws Exception {
    InsightConfig config = new InsightConfig();
    HstsConfig hstsConfig = config.getHstsConfig();
    hstsConfig.setEnabled(true);
    hstsConfig.setIncludeSubDomains(false);
    hstsConfig.setMaxAgeSeconds(3600);

    HstsHeaderFilter filter = new HstsHeaderFilter(config);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSecure(true);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("Strict-Transport-Security")).isEqualTo("max-age=3600");
  }
}
