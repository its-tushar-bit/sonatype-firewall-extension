/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgress;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data access object for managing cascade re-evaluation progress tracking.
 *
 * @since 1.196
 */
@Named
@Singleton
public class ReevaluateCascadeProgressDAO
    extends AbstractOperationalSqlDAO<ReevaluateCascadeProgress>
{
  private static final Logger log = LoggerFactory.getLogger(ReevaluateCascadeProgressDAO.class);

  @Inject
  public ReevaluateCascadeProgressDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<ReevaluateCascadeProgress> getByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  public List<ReevaluateCascadeProgress> getByRequestId(
      final TransactionContext tx,
      final String reevaluateCascadeRequestId)
  {
    String sQuery = "SELECT entity FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.reevaluateCascadeRequestId=?1 ORDER BY entity.id";
    return getList(tx, sQuery, reevaluateCascadeRequestId);
  }

  public List<ReevaluateCascadeProgress> getByRepositoryId(final String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  public List<ReevaluateCascadeProgress> getByRepositoryId(
      final TransactionContext tx,
      final String repositoryId)
  {
    String sQuery = "SELECT entity FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.repositoryId=?1 ORDER BY entity.id DESC";
    return getList(tx, sQuery, repositoryId);
  }

  public List<ReevaluateCascadeProgress> getByRepositoryComponentId(final String repositoryComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryComponentId(tx, repositoryComponentId);
    }
  }

  public List<ReevaluateCascadeProgress> getByRepositoryComponentId(
      final TransactionContext tx,
      final String repositoryComponentId)
  {
    String sQuery = "SELECT entity FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.repositoryComponentId=?1 ORDER BY entity.id DESC";
    return getList(tx, sQuery, repositoryComponentId);
  }

  public long countPendingByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return countPendingByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  public long countPendingByRequestId(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    String sQuery = "SELECT COUNT(entity.id) FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.reevaluateCascadeRequestId=?1 AND entity.status=?2";
    return getSingle(tx, Number.class, sQuery, reevaluateCascadeRequestId,
        ReevaluateCascadeProgressStatus.PENDING).longValue();
  }

  public long countCompletedByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return countCompletedByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  public long countCompletedByRequestId(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    String sQuery = "SELECT COUNT(entity.id) FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.reevaluateCascadeRequestId=?1 AND entity.status=?2";
    return getSingle(tx, Number.class, sQuery, reevaluateCascadeRequestId,
        ReevaluateCascadeProgressStatus.COMPLETED).longValue();
  }

  public long countFailedByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return countFailedByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  public long countFailedByRequestId(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    String sQuery = "SELECT COUNT(entity.id) FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.reevaluateCascadeRequestId=?1 AND entity.status=?2";
    return getSingle(tx, Number.class, sQuery, reevaluateCascadeRequestId,
        ReevaluateCascadeProgressStatus.FAILED).longValue();
  }

  public boolean isRequestComplete(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return isRequestComplete(tx, reevaluateCascadeRequestId);
    }
  }

  public boolean isRequestComplete(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    long pendingCount = countPendingByRequestId(tx, reevaluateCascadeRequestId);
    boolean isComplete = pendingCount == 0;

    if (isComplete) {
      log.debug("Cascade request {} has been completed (no pending progress entries)",
          reevaluateCascadeRequestId);
    }

    return isComplete;
  }

  public void deleteByRequestIds(TransactionContext tx, Set<String> requestIds) {
    if (requestIds.isEmpty()) {
      return;
    }

    Iterable<List<String>> batches = Iterables.partition(requestIds, getInOperatorThreshold());

    String sQuery = "DELETE FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.reevaluateCascadeRequestId IN (?1)";

    for (List<String> batch : batches) {
      createQuery(sQuery, batch).executeUpdate(tx);
    }
  }
}
