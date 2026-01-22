/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestResult;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class SourceControlPullRequestResultDAO
    extends AbstractOperationalSqlDAO<SourceControlPullRequestResult>
{
  @Inject
  public SourceControlPullRequestResultDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<SourceControlPullRequestResult> getByApplicationId(TransactionContext tx, String applicationId) {
    String sQuery = "SELECT entity FROM SourceControlPullRequestResult entity" + //
        " WHERE entity.applicationId=?1";
    return getList(tx, sQuery, applicationId);
  }

  public List<SourceControlPullRequestResult> getByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, applicationId);
    }
  }

  public void deleteAll() {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();

      String sQuery = "DELETE FROM SourceControlPullRequestResult entity";
      createQuery(sQuery).executeUpdate(tx);

      tx.commit();
    }
  }

  @Override
  public final void delete(TransactionContext tx, SourceControlPullRequestResult entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting related entities.
    super.delete(tx, entity);
  }

  @Override
  public final void delete(SourceControlPullRequestResult entity) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting related entities.
    super.delete(entity);
  }
}
