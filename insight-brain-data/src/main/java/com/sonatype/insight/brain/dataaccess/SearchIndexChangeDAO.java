/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Condition;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SearchIndexChange.SEARCH_INDEX_CHANGE;

@Named
@Singleton
public class SearchIndexChangeDAO
    extends AbstractOperationalSqlDAO<SearchIndexChange>
{
  @Inject
  public SearchIndexChangeDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public int insert(TransactionContext tx, SearchIndexChange entity) {
    if (SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.isEnabled() &&
        SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.isEnabled())
    {
      if (entity.getCreatedAt() == null) {
        entity.setCreatedAt(new Date());
      }
      if (entity.getStatus() == null) {
        entity.setStatus(SearchIndexChange.STATUS_PENDING);
      }
      if (entity.getAttemptCount() == null) {
        entity.setAttemptCount(0);
      }
      if (entity.getAvailableAt() == null) {
        entity.setAvailableAt(entity.getCreatedAt());
      }
      return super.insert(tx, entity);
    }
    return 0;
  }

  @Override
  public int update(TransactionContext tx, SearchIndexChange entity) {
    throw new UnsupportedOperationException();
  }

  public List<SearchIndexChange> getBatch(final int limit) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SEARCH_INDEX_CHANGE)
          .where(drainable())
          .orderBy(SEARCH_INDEX_CHANGE.CREATED_AT.asc().nullsFirst(), SEARCH_INDEX_CHANGE.SEARCH_INDEX_CHANGE_ID.asc())
          .limit(limit)
          .fetchInto(SearchIndexChange.class);
    }
  }

  /**
   * Oldest change still waiting to be applied, or null when the queue is empty. Served by
   * {@code search_index_change_status_created_idx}. Run once per drained batch, never per change.
   */
  public Date findOldestPendingCreatedAt() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .select(DSL.min(SEARCH_INDEX_CHANGE.CREATED_AT))
          .from(SEARCH_INDEX_CHANGE)
          .where(drainable())
          .fetchOne(0, Date.class);
    }
  }

  /**
   * Changes still waiting to be applied. The queue depth lives in the outbox itself rather than in a
   * counter on {@code search_index_health}: keeping a counter meant every insert had to update one
   * shared row inside the caller's transaction, which serialised unrelated writers on a single lock.
   * Run once per drained batch and once per Analyze, never per change.
   */
  public long countPending() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl().fetchCount(SEARCH_INDEX_CHANGE, drainable());
    }
  }

  /**
   * Rows the indexer will pick up. A null status predates the status column.
   */
  private static Condition drainable() {
    return SEARCH_INDEX_CHANGE.STATUS.isNull()
        .or(SEARCH_INDEX_CHANGE.STATUS.eq(SearchIndexChange.STATUS_PENDING))
        .or(SEARCH_INDEX_CHANGE.STATUS.eq(SearchIndexChange.STATUS_FAILED));
  }

  @Override
  public Table<?> getJooqTable() {
    return SEARCH_INDEX_CHANGE;
  }

  @Override
  public Class<SearchIndexChange> getEntityClass() {
    return SearchIndexChange.class;
  }
}
