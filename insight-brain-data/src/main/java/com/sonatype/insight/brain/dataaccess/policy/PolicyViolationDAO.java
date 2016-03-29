/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
  static final int IN_OPERATOR_THRESHOLD = 2000;

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
    if (evaluationIds.size() >= IN_OPERATOR_THRESHOLD) {
      // As measurements have shown (cf. CLM-6085), H2 doesn't handle an {@code IN} operator with a huge list of values
      // well and query time increases superlinear. Making multiple queries with smaller chunks of the input set keeps
      // the performance more linear. The chunk size below has been found to be a good compromise between DB query
      // overhead and individual query time.
      List<PolicyViolation> violations = new ArrayList<>(evaluationIds.size());
      int chunkSize = 200;
      List<String> evalIds = new ArrayList<>(chunkSize);
      for (String evaluationId : evaluationIds) {
        evalIds.add(evaluationId);
        if (evalIds.size() >= chunkSize) {
          violations.addAll(getList(sQuery, evalIds));
          evalIds.clear();
        }
      }
      if (!evalIds.isEmpty()) {
        violations.addAll(getList(sQuery, evalIds));
      }
      return violations;
    }
    return getList(sQuery, evaluationIds);
  }

  public List<PolicyViolation> getFirstOccurrenceByApplicationIdAndStageTypeId(TransactionContext tx,
                                                                               String appId,
                                                                               String stageTypeId)
  {
    String sQuery = "SELECT policyViolation" + //
        " FROM PolicyViolation policyViolation," + //
        "   FirstOccurrencePolicyViolation firstOccurrencePolicyViolation" + //
        " WHERE policyViolation.id=firstOccurrencePolicyViolation.id" + //
        "   AND firstOccurrencePolicyViolation.applicationId=?1" + //
        "   AND firstOccurrencePolicyViolation.stageTypeId=?2";
    return getList(tx, sQuery, appId, stageTypeId);
  }

  public List<PolicyViolation> getFirstOccurrenceByApplicationIdAndStageTypeId(String appId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getFirstOccurrenceByApplicationIdAndStageTypeId(tx, appId, stageTypeId);
    }
  }

  public List<PolicyViolation> getFirstOccurrenceByApplicationIdAndStageTypeIdAndHash(String appId,
                                                                                      String stageTypeId,
                                                                                      String hash)
  {
    if (hash == null) {
      // unhashed components can cause violations but can't be tracked specifically
      return Collections.emptyList();
    }

    /*
     * While this can be done via a single JOIN query at the DB layer, analysis has shown this to be inefficiently
     * executed by H2 (cf. CLM-4703). Hence we gather the data via two separate queries.
     */
    try (TransactionContext tx = createTransactionContext()) {
      List<FirstOccurrencePolicyViolation> firstOccurrences = new FirstOccurrencePolicyViolationDAO()
          .getByApplicationIdAndStageId(tx, appId, stageTypeId);
      Collection<String> violationIds = new ArrayList<>();
      for (FirstOccurrencePolicyViolation firstOccurrence : firstOccurrences) {
        violationIds.add(firstOccurrence.getId());
      }

      String sQuery = "SELECT entity FROM PolicyViolation entity" + //
          " WHERE entity.hash = ?1 AND entity.id IN (?2)" + //
          " ORDER BY entity.policyId";
      return getList(tx, sQuery, hash, violationIds);
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

  public int replacePolicyId(String fromPolicyId, String toPolicyId) {
    String sQuery = "UPDATE PolicyViolation entity" + //
        " SET entity.policyId=?2" + //
        " WHERE entity.policyId=?1";
    Query query = createQuery(sQuery, fromPolicyId, toPolicyId);
    return query.executeUpdate();
  }

  public int replacePolicyId(TransactionContext tx, String applicationId, String fromPolicyId, String toPolicyId) {
    String sQuery = "UPDATE PolicyViolation entity" + //
        " SET entity.policyId=?3" + //
        " WHERE entity.policyId=?2 AND entity.policyEvaluationId IN" + //
        " (SELECT evaluation.id FROM PolicyEvaluation evaluation WHERE evaluation.applicationId=?1)";
    Query query = createQuery(sQuery, applicationId, fromPolicyId, toPolicyId);
    return query.executeUpdate(tx);
  }
}
