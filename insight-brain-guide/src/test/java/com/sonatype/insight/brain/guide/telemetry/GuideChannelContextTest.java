/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.After;
import org.junit.Test;

public class GuideChannelContextTest
{
  @After
  public void tearDown() {
    GuideChannelContext.clear();
  }

  @Test
  public void defaultsToApiWhenUnset() {
    assertThat(GuideChannelContext.getOrDefault()).isEqualTo(GuideChannel.API);
  }

  @Test
  public void returnsSetValueThenClears() {
    GuideChannelContext.set(GuideChannel.MCP);
    assertThat(GuideChannelContext.getOrDefault()).isEqualTo(GuideChannel.MCP);
    GuideChannelContext.clear();
    assertThat(GuideChannelContext.getOrDefault()).isEqualTo(GuideChannel.API);
  }
}
