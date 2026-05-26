/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.service.InsightConfig;
import jakarta.servlet.FilterChain;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class FrameOptionsHeaderFilterTest
{
  @Test
  public void shouldSkipHeaderByDefault() throws Exception {
    MockHttpServletResponse response = applyFilter(new InsightConfig());

    assertThat(response.getHeader(FrameOptionsHeaderFilter.HEADER_NAME)).isNull();
  }

  @Test
  public void shouldAddDenyHeaderWhenEnabled() throws Exception {
    InsightConfig config = new InsightConfig();
    config.getFrameOptionsConfig().setEnabled(true);

    MockHttpServletResponse response = applyFilter(config);

    assertThat(response.getHeader(FrameOptionsHeaderFilter.HEADER_NAME))
        .isEqualTo(FrameOptionsHeaderFilter.HEADER_VALUE);
  }

  @Test
  public void shouldHonorConfiguredFrameOptionsValue() throws Exception {
    InsightConfig config = new InsightConfig();
    config.getFrameOptionsConfig().setEnabled(true);
    config.getFrameOptionsConfig().setOption(InsightConfig.FrameOptionsConfig.FrameOption.SAMEORIGIN);

    MockHttpServletResponse response = applyFilter(config);

    assertThat(response.getHeader(FrameOptionsHeaderFilter.HEADER_NAME)).isEqualTo("SAMEORIGIN");
  }

  private MockHttpServletResponse applyFilter(final InsightConfig config) throws Exception {
    FrameOptionsHeaderFilter filter = new FrameOptionsHeaderFilter(config);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    Mockito.verify(chain).doFilter(request, response);
    return response;
  }
}
