/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgress;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeProgressStatus;
import com.sonatype.insight.dataaccess.TransactionContext;

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

  /**
   * Finds all progress entries for a given cascade request ID.
   */
  public List<ReevaluateCascadeProgress> getByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  /**
   * Finds all progress entries for a given cascade request ID.
   */
  public List<ReevaluateCascadeProgress> getByRequestId(
      final TransactionContext tx,
      final String reevaluateCascadeRequestId)
  {
    String sQuery = "SELECT entity FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.reevaluateCascadeRequestId=?1 ORDER BY entity.id";
    return getList(tx, sQuery, reevaluateCascadeRequestId);
  }

  /**
   * Finds progress entries by repository ID.
   */
  public List<ReevaluateCascadeProgress> getByRepositoryId(final String repositoryId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryId(tx, repositoryId);
    }
  }

  /**
   * Finds progress entries by repository ID.
   */
  public List<ReevaluateCascadeProgress> getByRepositoryId(
      final TransactionContext tx,
      final String repositoryId)
  {
    String sQuery = "SELECT entity FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.repositoryId=?1 ORDER BY entity.id DESC";
    return getList(tx, sQuery, repositoryId);
  }

  /**
   * Finds progress entries by repository component ID.
   */
  public List<ReevaluateCascadeProgress> getByRepositoryComponentId(final String repositoryComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByRepositoryComponentId(tx, repositoryComponentId);
    }
  }

  /**
   * Finds progress entries by repository component ID.
   */
  public List<ReevaluateCascadeProgress> getByRepositoryComponentId(
      final TransactionContext tx,
      final String repositoryComponentId)
  {
    String sQuery = "SELECT entity FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.repositoryComponentId=?1 ORDER BY entity.id DESC";
    return getList(tx, sQuery, repositoryComponentId);
  }

  /**
   * Counts pending progress entries for a given cascade request.
   */
  public long countPendingByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return countPendingByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  /**
   * Counts pending progress entries for a given cascade request.
   */
  public long countPendingByRequestId(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    String sQuery = "SELECT COUNT(entity.id) FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.reevaluateCascadeRequestId=?1 AND entity.status=?2";
    return getSingle(tx, Number.class, sQuery, reevaluateCascadeRequestId,
        ReevaluateCascadeProgressStatus.PENDING).longValue();
  }

  /**
   * Counts completed progress entries for a given cascade request.
   */
  public long countCompletedByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return countCompletedByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  /**
   * Counts completed progress entries for a given cascade request.
   */
  public long countCompletedByRequestId(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    String sQuery = "SELECT COUNT(entity.id) FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.reevaluateCascadeRequestId=?1 AND entity.status=?2";
    return getSingle(tx, Number.class, sQuery, reevaluateCascadeRequestId,
        ReevaluateCascadeProgressStatus.COMPLETED).longValue();
  }

  /**
   * Counts failed progress entries for a given cascade request.
   */
  public long countFailedByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return countFailedByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  /**
   * Counts failed progress entries for a given cascade request.
   */
  public long countFailedByRequestId(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    String sQuery = "SELECT COUNT(entity.id) FROM ReevaluateCascadeProgress entity" +
        " WHERE entity.reevaluateCascadeRequestId=?1 AND entity.status=?2";
    return getSingle(tx, Number.class, sQuery, reevaluateCascadeRequestId,
        ReevaluateCascadeProgressStatus.FAILED).longValue();
  }

  /**
   * Checks if all progress entries for a cascade request have been completed (either COMPLETED or FAILED).
   */
  public boolean isRequestComplete(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return isRequestComplete(tx, reevaluateCascadeRequestId);
    }
  }

  /**
   * Checks if all progress entries for a cascade request have been completed (either COMPLETED or FAILED).
   */
  public boolean isRequestComplete(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    long pendingCount = countPendingByRequestId(tx, reevaluateCascadeRequestId);
    boolean isComplete = pendingCount == 0;

    if (isComplete) {
      log.debug("Cascade request {} has been completed (no pending progress entries)",
          reevaluateCascadeRequestId);
    }

    return isComplete;
  }
}
