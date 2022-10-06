/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestResult;
import com.sonatype.insight.dataaccess.TransactionContext;

public class SourceControlPullRequestResultDAO
    extends AbstractOperationalSqlDAO<SourceControlPullRequestResult>
{
  public List<SourceControlPullRequestResult> getAll(TransactionContext tx) {
    String sQuery = "SELECT entity FROM SourceControlPullRequestResult entity";
    return getList(tx, sQuery);
  }

  public List<SourceControlPullRequestResult> getAll() {
    try (TransactionContext tx = createTransactionContext()) {
      return getAll(tx);
    }
  }

  @Override
  public SourceControlPullRequestResult getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM SourceControlPullRequestResult entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
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

  public void deleteByApplicationId(TransactionContext tx, String applicationId) {
    String sQuery = "DELETE FROM SourceControlPullRequestResult entity" + //
        " WHERE entity.applicationId=?1";
    createQuery(sQuery, applicationId).executeUpdate(tx);
  }

  public void deleteByApplicationId(String applicationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByApplicationId(tx, applicationId);
      tx.commit();
    }
  }

  public void deleteAll() {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteAll(tx);
      tx.commit();
    }
  }

  public void deleteAll(TransactionContext tx) {
    String sQuery = "DELETE FROM SourceControlPullRequestResult entity";
    createQuery(sQuery).executeUpdate(tx);
  }
}
