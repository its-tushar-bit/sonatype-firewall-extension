/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OrderingStrategy} factory methods (CLM-40039 Section 6.1).
 */
public class OrderingStrategyTest
{
  @Test
  public void testPriorityFor_newestFirstAssignsStrictlyDecreasingPriorities() {
    OrderingStrategy ordering = OrderingStrategy.newestFirst();
    long p0 = ordering.priorityFor(0);
    long p1 = ordering.priorityFor(1);
    long p2 = ordering.priorityFor(2);
    assertThat(p0).isEqualTo(Long.MAX_VALUE);
    assertThat(p1).isEqualTo(Long.MAX_VALUE - 1);
    assertThat(p2).isEqualTo(Long.MAX_VALUE - 2);
    assertThat(p0).isGreaterThan(p1).isGreaterThan(p2);
  }

  @Test
  public void testPriorityFor_newestFirstHandlesLargePositionWithoutOverflow() {
    OrderingStrategy ordering = OrderingStrategy.newestFirst();
    long large = ordering.priorityFor(1_000_000);
    assertThat(large).isEqualTo(Long.MAX_VALUE - 1_000_000L);
  }

  @Test
  public void testPriorityFor_fifoReturnsConstantZero() {
    OrderingStrategy ordering = OrderingStrategy.fifo();
    assertThat(ordering.priorityFor(0)).isEqualTo(0L);
    assertThat(ordering.priorityFor(1)).isEqualTo(0L);
    assertThat(ordering.priorityFor(999)).isEqualTo(0L);
  }
}
