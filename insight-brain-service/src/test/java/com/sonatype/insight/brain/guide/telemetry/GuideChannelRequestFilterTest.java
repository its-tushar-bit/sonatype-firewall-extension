/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class GuideChannelRequestFilterTest
{
  private final GuideChannelRequestFilter filter = new GuideChannelRequestFilter();

  @AfterEach
  public void tearDown() {
    GuideChannelContext.clear();
  }

  private ContainerRequestContext guideRequest(final String header) {
    ContainerRequestContext ctx = mock(ContainerRequestContext.class);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getPath()).thenReturn("api/v2/guide/components/detail");
    when(ctx.getUriInfo()).thenReturn(uriInfo);
    when(ctx.getHeaderString(GuideChannelRequestFilter.CLIENT_HEADER)).thenReturn(header);
    return ctx;
  }

  @Test
  public void uiHeaderSetsUiChannelOnGuidePath() {
    filter.filter(guideRequest("ui"));
    assertThat(GuideChannelContext.getOrDefault()).isEqualTo(GuideChannel.UI);
  }

  @Test
  public void missingHeaderSetsApiChannelOnGuidePath() {
    filter.filter(guideRequest(null));
    assertThat(GuideChannelContext.getOrDefault()).isEqualTo(GuideChannel.API);
  }

  @Test
  public void nonGuidePathDoesNotTouchChannel() {
    // pre-set a sentinel; a non-Guide request must early-return without overwriting it
    GuideChannelContext.set(GuideChannel.MCP);
    ContainerRequestContext ctx = mock(ContainerRequestContext.class);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getPath()).thenReturn("api/v2/applications");
    when(ctx.getUriInfo()).thenReturn(uriInfo);
    filter.filter(ctx);
    assertThat(GuideChannelContext.getOrDefault()).isEqualTo(GuideChannel.MCP); // unchanged
  }

  @Test
  public void responseFilterClearsChannel() {
    GuideChannelContext.set(GuideChannel.UI);
    filter.filter(mock(ContainerRequestContext.class), mock(jakarta.ws.rs.container.ContainerResponseContext.class));
    assertThat(GuideChannelContext.getOrDefault()).isEqualTo(GuideChannel.API); // back to default => cleared
  }
}
