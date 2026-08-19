/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.searchindex;

import java.util.Date;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SearchIndexHealth.SEARCH_INDEX_HEALTH;

@Named
@Singleton
public class SearchIndexHealthDAO
    extends AbstractOperationalSqlDAO<SearchIndexHealth>
{
  @Inject
  public SearchIndexHealthDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public SearchIndexHealth getCurrent() {
    return getById(SearchIndexHealth.CURRENT_ID);
  }

  /**
   * Returns the singleton health row, seeding it with the same defaults as the migration if it is
   * absent. A schema provisioned without that incremental would otherwise fail every read. Duplicate
   * keys are ignored so concurrent callers converge on one row rather than one of them erroring.
   */
  public SearchIndexHealth getOrSeedCurrent() {
    SearchIndexHealth existing = getCurrent();
    if (existing != null) {
      return existing;
    }
    SearchIndexHealth seed = new SearchIndexHealth();
    seed.setId(SearchIndexHealth.CURRENT_ID);
    seed.setHealthStatus(SearchIndexHealth.STATUS_HEALTHY);
    seed.setRecommendedOp(SearchIndexHealth.OP_NONE);
    seed.setQueueLagSeconds(0L);
    seed.setPendingChangeCount(0L);
    seed.setFailedChangeCount(0L);
    seed.setNouxUnlockState(SearchIndexHealth.UNLOCK_NOT_STARTED);
    seed.setUpdatedAt(new Date());
    try (TransactionContext tx = createTransactionContext()) {
      insert(tx, seed, true);
      tx.commit();
    }
    SearchIndexHealth seeded = getCurrent();
    return seeded != null ? seeded : seed;
  }

  /**
   * Adds to the running tally of changes the indexer gave up on. Unlike queue depth this is a
   * history of events rather than a property of the outbox, so it cannot be counted back out of any
   * table. Called once per drained batch.
   */
  public void recordAbandonedChanges(final long abandonedCount) {
    if (abandonedCount <= 0L) {
      return;
    }
    try (TransactionContext tx = createTransactionContext()) {
      Date now = new Date();
      tx.dsl()
          .update(SEARCH_INDEX_HEALTH)
          .set(SEARCH_INDEX_HEALTH.FAILED_CHANGE_COUNT, SEARCH_INDEX_HEALTH.FAILED_CHANGE_COUNT.plus(abandonedCount))
          .set(SEARCH_INDEX_HEALTH.FAILED_CHANGE_WINDOW_START,
              DSL.coalesce(SEARCH_INDEX_HEALTH.FAILED_CHANGE_WINDOW_START, now))
          .set(SEARCH_INDEX_HEALTH.UPDATED_AT, now)
          .where(SEARCH_INDEX_HEALTH.SEARCH_INDEX_HEALTH_ID.eq(SearchIndexHealth.CURRENT_ID))
          .execute();
      tx.commit();
    }
  }

  /**
   * Writes the whole derived block in one PK update. Queue depth and the oldest-pending pointer are
   * passed in already counted from the outbox rather than tracked incrementally, so they cannot
   * drift from the rows they describe.
   */
  public void updateDerivedStatus(
      final String healthStatus,
      final String recommendedOp,
      final long queueLagSeconds,
      final String activeJobId,
      final long pendingChangeCount,
      final Date oldestPendingCreatedAt)
  {
    try (TransactionContext tx = createTransactionContext()) {
      tx.dsl()
          .update(SEARCH_INDEX_HEALTH)
          .set(SEARCH_INDEX_HEALTH.HEALTH_STATUS, healthStatus)
          .set(SEARCH_INDEX_HEALTH.RECOMMENDED_OP, recommendedOp)
          .set(SEARCH_INDEX_HEALTH.QUEUE_LAG_SECONDS, queueLagSeconds)
          .set(SEARCH_INDEX_HEALTH.ACTIVE_JOB_ID, activeJobId)
          .set(SEARCH_INDEX_HEALTH.PENDING_CHANGE_COUNT, pendingChangeCount)
          .set(SEARCH_INDEX_HEALTH.OLDEST_PENDING_CREATED_AT, oldestPendingCreatedAt)
          .set(SEARCH_INDEX_HEALTH.UPDATED_AT, new Date())
          .where(SEARCH_INDEX_HEALTH.SEARCH_INDEX_HEALTH_ID.eq(SearchIndexHealth.CURRENT_ID))
          .execute();
      tx.commit();
    }
  }

  public void setActiveJobId(final String activeJobId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.dsl()
          .update(SEARCH_INDEX_HEALTH)
          .set(SEARCH_INDEX_HEALTH.ACTIVE_JOB_ID, activeJobId)
          .set(SEARCH_INDEX_HEALTH.UPDATED_AT, new Date())
          .where(SEARCH_INDEX_HEALTH.SEARCH_INDEX_HEALTH_ID.eq(SearchIndexHealth.CURRENT_ID))
          .execute();
      tx.commit();
    }
  }

  /**
   * Clears the failed tally. A rebuild reconstructs every document from source, so failures against
   * the previous index no longer describe the current one. Without this the count only ever grows
   * and a tenant stays pinned to WARNING or NOT_HEALTHY for the life of the install.
   */
  public void resetFailedChanges() {
    try (TransactionContext tx = createTransactionContext()) {
      tx.dsl()
          .update(SEARCH_INDEX_HEALTH)
          .set(SEARCH_INDEX_HEALTH.FAILED_CHANGE_COUNT, 0L)
          .setNull(SEARCH_INDEX_HEALTH.FAILED_CHANGE_WINDOW_START)
          .set(SEARCH_INDEX_HEALTH.UPDATED_AT, new Date())
          .where(SEARCH_INDEX_HEALTH.SEARCH_INDEX_HEALTH_ID.eq(SearchIndexHealth.CURRENT_ID))
          .execute();
      tx.commit();
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SEARCH_INDEX_HEALTH;
  }

  @Override
  public Class<SearchIndexHealth> getEntityClass() {
    return SearchIndexHealth.class;
  }
}
