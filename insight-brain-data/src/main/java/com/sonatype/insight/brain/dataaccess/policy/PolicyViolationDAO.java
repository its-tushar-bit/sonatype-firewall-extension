/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.11
 */
public class PolicyViolationDAO
    extends AbstractOperationalSqlDAO<PolicyViolation>
{
  @Override
  protected PolicyViolation getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<PolicyViolation> getActiveByEvaluationId(String evaluationId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyEvaluationId=?1 AND entity.isWaived=false" + //
        " ORDER BY entity.policyId, entity.hash";
    return getList(sQuery, evaluationId);
  }

  public List<PolicyViolation> getByEvaluationId(String evaluationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByEvaluationId(tx, evaluationId);
    }
  }

  public List<PolicyViolation> getByEvaluationId(TransactionContext tx, String evaluationId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyEvaluationId=?1" + //
        " ORDER BY entity.policyId, entity.hash";
    return getList(tx, sQuery, evaluationId);
  }

  public List<PolicyViolation> getActiveByEvaluationIds(Set<String> evaluationIds) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyEvaluationId IN (?1) AND entity.isWaived=false";
    return getList(sQuery, evaluationIds);
  }

  public List<PolicyViolation> getFirstOccurrenceByApplicationIdAndStageTypeId(TransactionContext tx, String appId,
      String stageTypeId)
  {
    String sQuery = "SELECT policyViolation" + //
        " FROM PolicyViolation policyViolation," + //
        "   FirstOccurrencePolicyViolation firstOccurrencePolicyViolation" + //
        " WHERE policyViolation.id=firstOccurrencePolicyViolation.id" + //
        "   AND firstOccurrencePolicyViolation.applicationId=?1" + //
        "   AND firstOccurrencePolicyViolation.stageTypeId=?2" + //
        " ORDER BY policyViolation.policyId, policyViolation.hash";
    return getList(tx, sQuery, appId, stageTypeId);
  }

  public List<PolicyViolation> getFirstOccurrenceByApplicationIdAndStageTypeId(String appId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getFirstOccurrenceByApplicationIdAndStageTypeId(tx, appId, stageTypeId);
    }
  }

  @Override
  public void delete(TransactionContext tx, PolicyViolation entity) {
    // Cascade to first occurrence policy violation
    FirstOccurrencePolicyViolationDAO firstOccurrencePolicyViolationDAO = new FirstOccurrencePolicyViolationDAO();
    FirstOccurrencePolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationDAO.getById(tx,
        entity.getId());
    if (firstOccurrencePolicyViolation != null) {
      firstOccurrencePolicyViolationDAO.delete(tx, firstOccurrencePolicyViolation);
    }

    // Cascade to waived policy violation
    if (entity.isWaived()) {
      WaivedPolicyViolationDAO waivedPolicyViolationDAO = new WaivedPolicyViolationDAO();
      WaivedPolicyViolation waivedPolicyViolation = waivedPolicyViolationDAO.getById(tx, entity.getId());
      if (waivedPolicyViolation != null) {
        waivedPolicyViolationDAO.delete(tx, waivedPolicyViolation);
      }
    }

    super.delete(tx, entity);
  }

  public List<PolicyViolation> getActiveByEvaluationIdAndHash(String evaluationId, String hash) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyEvaluationId=?1 AND entity.hash=?2 AND entity.isWaived=false" + //
        " ORDER BY entity.policyId";
    return getList(sQuery, evaluationId, hash);
  }

  /**
   * Gets the violation corresponding to the first occurrence of the given (supposedly recent) violation.
   */
  public PolicyViolation getFirstOccurrence(String applicationId, String stageTypeId, PolicyViolation violation) {
    if (violation.getHash() == null) {
      return violation;
    }

    String sQuery = "SELECT policyViolation" + //
        " FROM PolicyViolation policyViolation, FirstOccurrencePolicyViolation firstOccurrencePolicyViolation" + //
        " WHERE policyViolation.id=firstOccurrencePolicyViolation.id" + //
        " AND firstOccurrencePolicyViolation.applicationId=?1 AND firstOccurrencePolicyViolation.stageTypeId=?2" + //
        " AND policyViolation.policyId=?3 AND policyViolation.hash=?4";
    PolicyViolation firstViolation = get(sQuery, applicationId, stageTypeId, violation.getPolicyId(),
        violation.getHash());
    if (firstViolation == null) {
      /*
       * Incomplete data migration between snapshot builds might prevent us from accurately detecting the first
       * occurrence. In that case, we take the current violation as the first occurrence, the show must go on.
       */
      firstViolation = violation;
    }
    return firstViolation;
  }


}
