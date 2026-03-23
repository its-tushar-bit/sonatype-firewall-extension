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
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ReevaluateCascadeProgress.REEVALUATE_CASCADE_PROGRESS;

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
    return tx.dsl()
        .selectFrom(REEVALUATE_CASCADE_PROGRESS)
        .where(REEVALUATE_CASCADE_PROGRESS.REEVALUATE_CASCADE_REQUEST_ID.eq(reevaluateCascadeRequestId))
        .orderBy(REEVALUATE_CASCADE_PROGRESS.REEVALUATE_CASCADE_PROGRESS_ID)
        .fetch(super::toEntity);
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
    return tx.dsl()
        .selectFrom(REEVALUATE_CASCADE_PROGRESS)
        .where(REEVALUATE_CASCADE_PROGRESS.REPOSITORY_ID.eq(repositoryId))
        .orderBy(REEVALUATE_CASCADE_PROGRESS.REEVALUATE_CASCADE_PROGRESS_ID.desc())
        .fetch(super::toEntity);
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
    return tx.dsl()
        .selectFrom(REEVALUATE_CASCADE_PROGRESS)
        .where(REEVALUATE_CASCADE_PROGRESS.REPOSITORY_COMPONENT_ID.eq(repositoryComponentId))
        .orderBy(REEVALUATE_CASCADE_PROGRESS.REEVALUATE_CASCADE_PROGRESS_ID.desc())
        .fetch(super::toEntity);
  }

  public long countPendingByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return countPendingByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  public long countPendingByRequestId(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    return tx.dsl()
        .selectCount()
        .from(REEVALUATE_CASCADE_PROGRESS)
        .where(REEVALUATE_CASCADE_PROGRESS.REEVALUATE_CASCADE_REQUEST_ID.eq(reevaluateCascadeRequestId))
        .and(REEVALUATE_CASCADE_PROGRESS.STATUS.eq(ReevaluateCascadeProgressStatus.PENDING.name()))
        .fetchOne(0, Long.class);
  }

  public long countCompletedByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return countCompletedByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  public long countCompletedByRequestId(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    return tx.dsl()
        .selectCount()
        .from(REEVALUATE_CASCADE_PROGRESS)
        .where(REEVALUATE_CASCADE_PROGRESS.REEVALUATE_CASCADE_REQUEST_ID.eq(reevaluateCascadeRequestId))
        .and(REEVALUATE_CASCADE_PROGRESS.STATUS.eq(ReevaluateCascadeProgressStatus.COMPLETED.name()))
        .fetchOne(0, Long.class);
  }

  public long countFailedByRequestId(final String reevaluateCascadeRequestId) {
    try (TransactionContext tx = createTransactionContext()) {
      return countFailedByRequestId(tx, reevaluateCascadeRequestId);
    }
  }

  public long countFailedByRequestId(final TransactionContext tx, final String reevaluateCascadeRequestId) {
    return tx.dsl()
        .selectCount()
        .from(REEVALUATE_CASCADE_PROGRESS)
        .where(REEVALUATE_CASCADE_PROGRESS.REEVALUATE_CASCADE_REQUEST_ID.eq(reevaluateCascadeRequestId))
        .and(REEVALUATE_CASCADE_PROGRESS.STATUS.eq(ReevaluateCascadeProgressStatus.FAILED.name()))
        .fetchOne(0, Long.class);
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

    for (List<String> batch : batches) {
      tx.dsl()
          .deleteFrom(REEVALUATE_CASCADE_PROGRESS)
          .where(REEVALUATE_CASCADE_PROGRESS.REEVALUATE_CASCADE_REQUEST_ID.in(batch))
          .execute();
    }
  }

  @Override
  public void delete(TransactionContext tx, ReevaluateCascadeProgress entity) {
    tx.dsl()
        .deleteFrom(REEVALUATE_CASCADE_PROGRESS)
        .where(REEVALUATE_CASCADE_PROGRESS.REEVALUATE_CASCADE_PROGRESS_ID.eq(entity.getId()))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return REEVALUATE_CASCADE_PROGRESS;
  }

  @Override
  public Class<ReevaluateCascadeProgress> getEntityClass() {
    return ReevaluateCascadeProgress.class;
  }
}
