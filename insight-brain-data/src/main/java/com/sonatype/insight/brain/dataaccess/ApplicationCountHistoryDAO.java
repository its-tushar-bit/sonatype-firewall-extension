/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess;

import java.util.Date;
import java.util.Optional;

import com.sonatype.insight.brain.model.ApplicationCountHistory;
import com.sonatype.insight.dataaccess.TransactionContext;

public class ApplicationCountHistoryDAO
    extends AbstractOperationalSqlDAO<ApplicationCountHistory>
{
  private ApplicationDAO appDAO = new ApplicationDAO();

  @Override
  public void update(TransactionContext tx, ApplicationCountHistory entity) {
    throw new UnsupportedOperationException("ApplicationCountHistory does not support update operations");
  }

  private int getInitialApplicationCount() {
    String sQuery = "SELECT entity" +
        " FROM ApplicationCountHistory entity" +
        " WHERE entity.id = 'initialization'";
    Query<ApplicationCountHistory> query = createQuery(sQuery);
    query.forceSingleResult();
    ApplicationCountHistory applicationCountHistory = query.get();
    return applicationCountHistory.getApplicationCount();
  }

  private Optional<Integer> getApplicationCountAt(Date timestamp) {
    String sQuery = "SELECT entity" +
        " FROM ApplicationCountHistory entity" +
        " WHERE entity.updatedDate <= ?1" +
        " ORDER BY entity.updatedDate DESC";
    Query<ApplicationCountHistory> query = createQuery(sQuery, timestamp);

    query.forceSingleResult();
    Optional<Integer> countIfFound =
        query.getList().stream().findFirst().map(ApplicationCountHistory::getApplicationCount);
    return countIfFound;
  }

  public int getApplicationCountAtOrDefault(Date timestamp) {
    return getApplicationCountAt(timestamp).orElseGet(this::getInitialApplicationCount);
  }

  public void recordApplicationCount() {
    int currentAppCount = (int) appDAO.getCount();
    ApplicationCountHistory countHistory = new ApplicationCountHistory(currentAppCount, new Date());
    insert(countHistory);
  }
}
