/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.eventbus;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventBusConfigTest
{
  @Test
  public void testGetEventBusPoolSize_Default() throws Exception {
    final EventBusConfig underTest = new EventBusConfig();
    assertThat(underTest.getMaxPoolSize()).isEqualTo(500);
  }

  @Test
  public void testGetEventBusPoolSize() throws Exception {
    final EventBusConfig underTest = new EventBusConfig();
    underTest.setMaxPoolSize(25);
    assertThat(underTest.getMaxPoolSize()).isEqualTo(25);
  }
}
