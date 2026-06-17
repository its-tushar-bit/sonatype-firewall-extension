/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * {@link EligibilitySelector} for hosted-repository continuous monitoring (CLM-40039 Section 6.1).
 * Returns repository components from monitoring-enabled hosted repositories whose
 * {@code last_evaluation_time} predates the cycle start, ordered newest-first.
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
  public List<RepositoryComponent> fetchPage(final int offset, final int limit, final Instant cycleStart) {
    try (TransactionContext tx = repositoryComponentDAO.createTransactionContext()) {
      return repositoryComponentDAO.getMonitoringEligiblePage(tx, Date.from(cycleStart), limit, offset);
    }
  }
}
