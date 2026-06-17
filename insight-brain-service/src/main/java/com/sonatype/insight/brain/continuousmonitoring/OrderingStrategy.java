/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

/**
 * Computes the queue priority for a candidate at enqueue time. Per the unified continuous
 * monitoring queue design (CLM-40039, Section 4.1 / Decision #4), each flow chooses how rows are
 * ordered for the consumer:
 * <ul>
 * <li>{@link #newestFirst()} — assigns strictly decreasing priorities so newer candidates
 * (earlier in the producer's cycle stream) are acquired first;</li>
 * <li>{@link #fifo()} — assigns a constant priority so rows are consumed in insertion order
 * (queue table's secondary key on {@code create_time}).</li>
 * </ul>
 * The strategy is consulted once per candidate during the producer cycle; the resulting
 * {@code long} is written to {@code continuous_monitoring_queue.priority} and the consumer's
 * acquire query orders {@code priority DESC, create_time ASC}.
 */
public interface OrderingStrategy
{
  /**
   * @param positionInCycle 0-based position of this candidate in the producer's eligibility
   *          stream for the current cycle (page-aware: page 2 row 0 has positionInCycle =
   *          pageSize, etc.).
   * @return the value to store in {@code continuous_monitoring_queue.priority}.
   */
  long priorityFor(long positionInCycle);

  /**
   * Newest-first: the first candidate in a cycle gets the highest priority. Implemented by
   * subtracting {@code positionInCycle} from a fixed anchor so values fit in a {@code long} and
   * decrease monotonically. Used by the Hosted Repo flow whose eligibility query orders by
   * {@code repository_component.time DESC} so newly-added components are processed first.
   */
  static OrderingStrategy newestFirst() {
    return positionInCycle -> Long.MAX_VALUE - positionInCycle;
  }

  /** Constant priority — rows fall through to the {@code create_time ASC} secondary sort. */
  static OrderingStrategy fifo() {
    return positionInCycle -> 0L;
  }
}
