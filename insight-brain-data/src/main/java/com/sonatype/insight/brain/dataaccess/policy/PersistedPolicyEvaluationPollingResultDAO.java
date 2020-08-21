/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.dataaccess.TransactionContext;

public class PersistedPolicyEvaluationPollingResultDAO
    extends AbstractOperationalSqlDAO<PersistedPolicyEvaluationPollingResult>
{
  public PersistedPolicyEvaluationPollingResult getByApplicationIdAndStatusId(String applicationId, String statusId) {
    String sQuery = "SELECT entity FROM PersistedPolicyEvaluationPollingResult entity" + //
        " WHERE entity.applicationId=?1" + //
        " AND entity.statusId=?2";
    return get(sQuery, applicationId, statusId);
  }

  @Override
  public final void delete(
      TransactionContext tx,
      PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult)
  {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(tx, persistedPolicyEvaluationPollingResult);
  }

  @Override
  public final void delete(PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult) {
    // WARNING: Don't add any business logic to this method because, for performance reasons,
    // we bypass this method when deleting all expired entities.
    super.delete(persistedPolicyEvaluationPollingResult);
  }

  public void deleteBeforeOrOn(Date date) {
    String sQuery = "DELETE FROM PersistedPolicyEvaluationPollingResult entity" + //
        " WHERE entity.createTime <= ?1";
    createQuery(sQuery, date).executeUpdate();
  }

  public void deleteAll() {
    String sQuery = "DELETE FROM PersistedPolicyEvaluationPollingResult entity";
    createQuery(sQuery).executeUpdate();
  }
}
