/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.continuousmonitoring.EligibilityCursor;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.Page;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * {@link EligibilitySelector} for hosted-repository continuous monitoring (CLM-40039 Section 6.1,
 * CLM-41005 keyset pagination). Returns the hosted-repository components of monitoring-enabled hosted
 * repositories — one row per {@code (repository, pathname)} — keyset-advanced past the cursor.
 * <p>
 * Candidates come from {@code hosted_repository_component}, which owns a hosted artifact's identity
 * and therefore its monitoring eligibility. {@code cycleStart} is unused: a component's evaluation
 * history lives in {@code policy_evaluation}, not on this table, so there is no
 * {@code last_evaluation_time} here to filter on. Overlapping cycles are prevented structurally by
 * {@code @DisallowConcurrentExecution} on the producer job rather than by a time predicate, matching
 * how Lifecycle monitoring enumerates its work ({@code PolicyMonitor.evaluateApplications} pages
 * {@code applicationDAO.getAll(page, pageSize)} unfiltered).
 */
@Named
@Singleton
public class HostedRepoEligibilitySelector
    implements EligibilitySelector<HostedRepositoryComponent>
{
  /**
   * Filler for {@link EligibilityCursor#time()}, which is required non-null but plays no part in this
   * flow's keyset. Constant so cursors differ only by id.
   */
  private static final Date UNUSED_CURSOR_TIME = new Date(0L);

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  public HostedRepoEligibilitySelector(final HostedRepositoryComponentDAO hostedRepositoryComponentDAO) {
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
  }

  @Override
  public Page<HostedRepositoryComponent> fetchPage(
      @Nullable final EligibilityCursor cursor,
      final int limit,
      final Instant cycleStart)
  {
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      List<HostedRepositoryComponent> rows =
          hostedRepositoryComponentDAO.getMonitoringEligiblePage(tx, limit, cursor);
      if (rows.isEmpty()) {
        return Page.empty();
      }
      HostedRepositoryComponent last = rows.get(rows.size() - 1);
      // The keyset is the primary key alone — hosted_repository_component has no timestamp column — but
      // EligibilityCursor requires a non-null time it can encode(). Carry the epoch as a constant filler
      // so every cursor encodes identically on the time half and only the id advances; the DAO ignores it.
      EligibilityCursor nextCursor = new EligibilityCursor(UNUSED_CURSOR_TIME, last.getId());
      // Saturated page (size == limit) ⇒ probably more rows behind it. When the eligibility set
      // is an exact multiple of limit, this triggers one extra DAO round-trip (which returns an
      // empty Page, stopping the cycle) — intentional and benign: detecting end-of-stream without
      // it would require a second predicate per page that does not pay for itself. A short page
      // (size < limit) means we definitely hit the tail and the cycle terminates without the
      // trailing fetch.
      boolean hasMore = rows.size() == limit;
      return new Page<>(rows, nextCursor, hasMore);
    }
  }
}
