/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess;

import java.util.Date;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ApplicationCountHistory.APPLICATION_COUNT_HISTORY;

@Named
@Singleton
public class ApplicationCountHistoryDAO
    extends AbstractOperationalSqlDAO<ApplicationCountHistory>
{
  @Inject
  public ApplicationCountHistoryDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public int update(TransactionContext tx, ApplicationCountHistory entity) {
    throw new UnsupportedOperationException("ApplicationCountHistory does not support update operations");
  }

  @Override
  public Table<?> getJooqTable() {
    return APPLICATION_COUNT_HISTORY;
  }

  @Override
  public Class<ApplicationCountHistory> getEntityClass() {
    return ApplicationCountHistory.class;
  }

  // there should always be at least one entry as we will create an initial entry via the schema or migration
  public ApplicationCountHistory getInitialApplicationCountHistory() {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(APPLICATION_COUNT_HISTORY)
          .where(APPLICATION_COUNT_HISTORY.APPLICATION_COUNT_HISTORY_ID.eq("initialization"))
          .fetchOne());
    }
  }

  private Optional<Integer> getApplicationCountAt(Date timestamp) {
    final ApplicationCountHistory applicationCountHistory = getApplicationCountHistory(timestamp);

    if (applicationCountHistory == null) {
      return Optional.empty();
    }
    else {
      return Optional.of(applicationCountHistory.getApplicationCount());
    }
  }

  public ApplicationCountHistory getApplicationCountHistory(final Date date) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(APPLICATION_COUNT_HISTORY)
          .where(APPLICATION_COUNT_HISTORY.UPDATED_DATE.le(date))
          .orderBy(APPLICATION_COUNT_HISTORY.UPDATED_DATE.desc())
          .limit(1)
          .fetchOne());
    }
  }

  public int getApplicationCountAtOrDefault(Date timestamp) {
    return getApplicationCountAt(timestamp).orElseGet(this::getInitialApplicationCount);
  }

  private int getInitialApplicationCount() {
    return getInitialApplicationCountHistory().getApplicationCount();
  }
}
