/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.11
 */
public class PolicyEvaluationDAO
    extends AbstractOperationalSqlDAO<PolicyEvaluation>
{


  @Override
  protected PolicyEvaluation getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public PolicyEvaluation getLastMonitoringByApplicationIdAndScanId(String appId, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastMonitoringByApplicationIdAndScanId(tx, appId, scanId);
    }
  }

  public PolicyEvaluation getLastMonitoringByApplicationIdAndScanId(TransactionContext tx, String appId, String scanId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.scanId=?2 AND entity.isForMonitoring=true" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, appId, scanId).forceSingleResult().get(tx);
  }

  public PolicyEvaluation getLastByApplicationIdAndScanId(TransactionContext tx, String appId, String scanId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.scanId=?2" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, appId, scanId).forceSingleResult().get(tx);
  }

  public PolicyEvaluation getLastByApplicationIdAndScanId(String appId, String scanId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastByApplicationIdAndScanId(tx, appId, scanId);
    }
  }

  public List<PolicyEvaluation> getLastByApplicationIds(Set<String> appIds) {
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation lpe" + //
        " WHERE pe.id = lpe.policyEvaluationId" + //
        " AND lpe.applicationId in (?1)";
    return getList(sQuery, appIds);
  }

  /**
   * Returns the most recent policy evaluation for the most recent scan for the given application and stage.
   */
  public PolicyEvaluation getLastByApplicationIdAndStageId(TransactionContext tx, String appId, String stageTypeId) {
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation lpe" + //
        " WHERE pe.id = lpe.policyEvaluationId" + //
        " AND lpe.applicationId=?1" + //
        " AND lpe.stageTypeId=?2";
    return get(tx, sQuery, appId, stageTypeId);
  }

  public List<PolicyEvaluation> getLastByApplicationIdsAndStageIds(Set<String> appIds, Set<String> stageTypeIds) {
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation lpe" + //
        " WHERE pe.id = lpe.policyEvaluationId" + //
        " AND lpe.applicationId in (?1)" + //
        " AND lpe.stageTypeId in (?2)";
    return getList(sQuery, appIds, stageTypeIds);
  }

  public PolicyEvaluation getLastByApplicationIdAndStageId(String appId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastByApplicationIdAndStageId(tx, appId, stageTypeId);
    }
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given application and stage.
   */
  public PolicyEvaluation getLastPrimaryByApplicationIdAndStageId(TransactionContext tx, String appId, String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 AND entity.isReevaluation=false" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, appId, stageTypeId).forceSingleResult().get(tx);
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given application and stage.
   */
  public PolicyEvaluation getLastPrimaryByApplicationIdAndStageId(String appId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getLastPrimaryByApplicationIdAndStageId(tx, appId, stageTypeId);
    }
  }

  public List<PolicyEvaluation> getAllByApplicationIdAndStageId(String appId, String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2" + //
        " ORDER BY entity.time DESC";
    return getList(sQuery, appId, stageTypeId);
  }

  @Override
  public void insert(TransactionContext tx, PolicyEvaluation policyEvaluation) {
    validate(policyEvaluation);
    final LastPolicyEvaluationDAO lastPolicyEvaluationDAO = new LastPolicyEvaluationDAO();

    if (policyEvaluation.getTime() == null) {
      policyEvaluation.setTime(new Date());
    }
    super.insert(tx, policyEvaluation);

    //make sure the last policy eval is right
    lastPolicyEvaluationDAO.deleteForApplicationIdAndStageTypeId(tx, policyEvaluation.getApplicationId(),
        policyEvaluation.getStageTypeId());
    lastPolicyEvaluationDAO.insertIfPossibleLastPolicyEvaluation(tx, policyEvaluation.getApplicationId(),
        policyEvaluation.getStageTypeId());
  }


  public List<PolicyEvaluation> getByApplicationId(TransactionContext tx, String appId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1";
    return getList(tx, sQuery, appId);
  }

  public List<PolicyEvaluation> getByApplicationIdAndStageIds(String appId, Set<String> stageTypeIds) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationIdAndStageIds(tx, appId, stageTypeIds);
    }
  }

  public List<PolicyEvaluation> getByApplicationIdAndStageIds(TransactionContext tx, String appId,
      Set<String> stageTypeIds)
  {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId = ?1 AND entity.stageTypeId IN (?2)" + //
        "   AND entity.isForObsoleteScan = false" + //
        " ORDER BY entity.time";
    return getList(tx, sQuery, appId, stageTypeIds);
  }

  @Override
  public void update(TransactionContext tx, PolicyEvaluation entity) {
    throw new UnsupportedOperationException("The PolicyEvaluation table does not support update operations");
  }

  @Override
  public void delete(final TransactionContext tx, PolicyEvaluation entity) {
    final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    final LastPolicyEvaluationDAO lastPolicyEvaluationDAO = new LastPolicyEvaluationDAO();

    // Cascade to policy violations
    List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationId(tx, entity.getId());
    for (PolicyViolation policyViolation : policyViolations) {
      policyViolationDAO.delete(tx, policyViolation);
    }

    //cascade to LastPolicyEvaluation
    lastPolicyEvaluationDAO.deleteForApplicationIdAndStageTypeId(tx, entity.getApplicationId(),
        entity.getStageTypeId());

    //delete the eval itself
    super.delete(tx, entity);

    //insert if possible to LastPolicyEvaluation
    lastPolicyEvaluationDAO.insertIfPossibleLastPolicyEvaluation(tx, entity.getApplicationId(),
        entity.getStageTypeId());
  }

  private void validate(PolicyEvaluation policyEvaluation) {
    if (!policyEvaluation.isReevaluation() && policyEvaluation.isForObsoleteScan()) {
      throw new IllegalStateException("Primary evaluations cannot be for obsolete scans");
    }
  }
}
