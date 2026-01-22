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
  public void update(TransactionContext tx, ApplicationCountHistory entity) {
    throw new UnsupportedOperationException("ApplicationCountHistory does not support update operations");
  }

  // there should always be at least one entry as we will create an initial entry via the schema or migration
  public ApplicationCountHistory getInitialApplicationCountHistory() {
    String sQuery = "SELECT entity" +
        " FROM ApplicationCountHistory entity" +
        " WHERE entity.id = 'initialization'";
    Query<ApplicationCountHistory> query = createQuery(sQuery);
    query.forceSingleResult();

    return query.get();
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
    final String sQuery = "SELECT entity" +
        " FROM ApplicationCountHistory entity" +
        " WHERE entity.updatedDate <= ?1" +
        " ORDER BY entity.updatedDate DESC";

    final Query<ApplicationCountHistory> query = createQuery(sQuery, date);

    return query.forceSingleResult().getList().stream().findFirst().orElse(null);
  }

  public int getApplicationCountAtOrDefault(Date timestamp) {
    return getApplicationCountAt(timestamp).orElseGet(this::getInitialApplicationCount);
  }

  private int getInitialApplicationCount() {
    return getInitialApplicationCountHistory().getApplicationCount();
  }
}
