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
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * {@link EligibilitySelector} for hosted-repository continuous monitoring (CLM-40039 Section 6.1,
 * CLM-41005 keyset pagination). Returns repository components from monitoring-enabled hosted
 * repositories whose {@code last_evaluation_time} predates the cycle start, ordered newest-first
 * by {@code (time DESC, repository_component_id DESC)} and keyset-advanced past the cursor.
 */
@Named
@Singleton
public class HostedRepoEligibilitySelector
    implements EligibilitySelector<RepositoryComponent>
{
  private final RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  public HostedRepoEligibilitySelector(final RepositoryComponentDAO repositoryComponentDAO) {
    this.repositoryComponentDAO = repositoryComponentDAO;
  }

  @Override
  public Page<RepositoryComponent> fetchPage(
      @Nullable final EligibilityCursor cursor,
      final int limit,
      final Instant cycleStart)
  {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      List<RepositoryComponent> rows =
          repositoryComponentDAO.getMonitoringEligiblePage(tx, Date.from(cycleStart), limit, cursor);
      if (rows.isEmpty()) {
        return Page.empty();
      }
      RepositoryComponent last = rows.get(rows.size() - 1);
      // RepositoryComponent.getId() returns the repository_component_id PK column.
      EligibilityCursor nextCursor = new EligibilityCursor(last.getTime(), last.getId());
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
