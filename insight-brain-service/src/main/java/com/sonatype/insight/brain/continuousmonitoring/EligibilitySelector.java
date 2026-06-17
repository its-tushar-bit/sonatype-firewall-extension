/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Instant;
import java.util.List;

/**
 * Per-flow producer-side query that returns components eligible for the next continuous monitoring
 * cycle (CLM-40039, Section 6.1). Implementations decide what makes a candidate eligible (e.g.
 * Hosted Repo: monitoring-enabled repository AND {@code last_evaluation_time} strictly less than
 * {@code cycleStart} — i.e. the row was last evaluated before the current cycle started) and emit
 * candidates ordered as the flow requires (Hosted Repo: newest first).
 * <p>
 * The selector returns flow-typed candidates; a {@link ContinuousMonitoringFlowProcessor} on the
 * consumer side knows how to consume them. The framework treats them opaquely.
 *
 * @param <T> the flow-specific candidate type (carries the natural-key fields needed for the
 *          satellite row insert and any context the priority computation needs)
 */
public interface EligibilitySelector<T>
{
  /**
   * Fetches one page of eligible candidates. The producer pages through the result set with the
   * caller-controlled limit; pagination ends when the page is shorter than {@code limit}.
   *
   * @param offset 0-based row offset into the eligibility stream for this cycle
   * @param limit max rows to return; bound by {@code continuousMonitoringEligibilityPageSize}
   * @param cycleStart the producer's cycle anchor — selectors use this to <em>exclude</em> rows
   *          whose {@code last_evaluation_time} is at or after {@code cycleStart}; those were
   *          already evaluated in the current cycle and must not be re-enqueued
   * @return up to {@code limit} candidates. Implementations MUST return a non-null list; an empty
   *         list signals no more eligible work this cycle. The producer treats {@code null} the
   *         same as an empty list defensively, but implementations should not rely on that.
   */
  List<T> fetchPage(int offset, int limit, Instant cycleStart);
}
