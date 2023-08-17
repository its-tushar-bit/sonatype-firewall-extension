/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.policy.LastPolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.12
 */
public class LastPolicyEvaluationDAO
    extends AbstractOperationalSqlDAO<LastPolicyEvaluation>
{
  public void insertIfPossibleLastPolicyEvaluation(final TransactionContext tx,
                                                   final String applicationId,
                                                   final String stageTypeId)
  {
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    // see if there is a most recent value
    String sQuery = "SELECT e from PolicyEvaluation e " + //
        "WHERE e.applicationId = ?1 " + //
        "AND e.stageTypeId = ?2 " + //
        "AND e.isForObsoleteScan = false " + //
        "ORDER BY e.time DESC";

    PolicyEvaluation newestPolicyEvaluation = policyEvaluationDAO.createQuery(sQuery, applicationId, stageTypeId)
        .forceSingleResult().get(tx);
    if (newestPolicyEvaluation != null) {
      insert(tx, new LastPolicyEvaluation(newestPolicyEvaluation.getId(), newestPolicyEvaluation.getApplicationId(),
          newestPolicyEvaluation.getStageTypeId()));
    }
  }

  public LastPolicyEvaluation getByEvaluationId(final TransactionContext tx, final String evaluationId) {
    String sQuery = "SELECT entity FROM LastPolicyEvaluation entity" + //
        " WHERE entity.policyEvaluationId=?1";
    return createQuery(sQuery, evaluationId).forceSingleResult().get(tx);
  }

  public LastPolicyEvaluation getByEvaluationId(final String evaluationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByEvaluationId(tx, evaluationId);
    }
  }

  @Override
  public LastPolicyEvaluation getById(String evaluationId) {
    return getByEvaluationId(evaluationId);
  }

  public LastPolicyEvaluation getByApplicationIdAndStageTypeId(final String applicationId, final String stageTypeId) {
    String sQuery = "SELECT entity FROM LastPolicyEvaluation entity" + //
        " WHERE entity.applicationId = ?1" + //
        " AND entity.stageTypeId = ?2";
    return get(sQuery, applicationId, stageTypeId);
  }

  @Override
  public void update(TransactionContext tx, LastPolicyEvaluation entity) {
    throw new UnsupportedOperationException("The LastPolicyEvaluation table does not support update operations");
  }

  @Override
  protected SearchIndexChange newSearchIndexChangeForInsert(LastPolicyEvaluation entity) {
    return new SearchIndexChange(ChangeType.LAST_POLICY_EVALUATION,
        entity.getApplicationId() + ':' + entity.getStageTypeId());
  }
}
