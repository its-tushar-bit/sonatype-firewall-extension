/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Instant;

import com.sonatype.insight.brain.dataaccess.continuousmonitoring.EligibilityCursor;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.Page;

import jakarta.annotation.Nullable;

/**
 * Per-flow producer-side query that returns components eligible for the next continuous monitoring
 * cycle (CLM-40039 Section 6.1, CLM-41005 keyset pagination). Implementations decide what makes a
 * candidate eligible (e.g. Hosted Repo: monitoring-enabled repository AND
 * {@code last_evaluation_time} strictly less than {@code cycleStart}) and emit candidates ordered
 * as the flow requires (Hosted Repo: newest first by {@code (time DESC, repository_component_id
 * DESC)}). The producer pages with a {@link EligibilityCursor} on that ordering tuple, so paging
 * cost is O(limit) regardless of position and no rows are skipped under concurrent writes.
 * <p>
 * <strong>Contract:</strong> Implementations must never return a page with empty {@code rows()}
 * and {@code hasMore() == true}. Such a page would trigger the empty-rows early-exit in
 * {@link AbstractContinuousMonitoringProducerJob#runCycle}, returning {@code success(0)} and
 * bypassing the safety-net WARN — a buggy selector would silently escape detection. The
 * {@link Page} record's constructor enforces this invariant structurally (CLM-41005).
 * <p>
 * When there are no more rows, return {@code Page.empty()} (which has {@code hasMore() == false}).
 *
 * @param <T> the flow-specific candidate type (carries the natural-key fields needed for the
 *          satellite row insert and any context the priority computation needs)
 */
public interface EligibilitySelector<T>
{
  /**
   * Fetches one page of eligible candidates.
   *
   * @param cursor the last row consumed in the current cycle; {@code null} means "first page".
   *          The next page contains rows strictly less than this tuple in the flow's natural
   *          ordering.
   * @param limit max rows to return; bound by {@code continuousMonitoringEligibilityPageSize}.
   * @param cycleStart the producer's cycle anchor — selectors use this to <em>exclude</em> rows
   *          whose {@code last_evaluation_time} is at or after {@code cycleStart}; those were
   *          already evaluated in the current cycle and must not be re-enqueued.
   * @return a non-null {@link Page}. {@code page.hasMore() == false} signals end of stream.
   */
  Page<T> fetchPage(@Nullable EligibilityCursor cursor, int limit, Instant cycleStart);
}
