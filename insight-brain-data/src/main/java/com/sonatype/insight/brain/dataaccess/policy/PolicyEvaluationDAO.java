/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

/**
 * @since 1.11
 */
public class PolicyEvaluationDAO
    extends AbstractOperationalSqlDAO<PolicyEvaluation>
{


  @Override
  protected PolicyEvaluation getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public PolicyEvaluation getLastMonitoringByApplicationIdAndScanId(String appId, String scanId) {
    EntityManager em = createEntityManager();
    try {
      return getLastMonitoringByApplicationIdAndScanId(em, appId, scanId);
    }
    finally {
      close(em);
    }
  }

  public PolicyEvaluation getLastMonitoringByApplicationIdAndScanId(EntityManager em, String appId, String scanId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.scanId=?2 AND entity.isForMonitoring=true" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, appId, scanId).forceSingleResult().get(em);
  }

  public PolicyEvaluation getLastByApplicationIdAndScanId(EntityManager em, String appId, String scanId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.scanId=?2" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, appId, scanId).forceSingleResult().get(em);
  }

  public PolicyEvaluation getLastByApplicationIdAndScanId(String appId, String scanId) {
    EntityManager em = createEntityManager();
    try {
      return getLastByApplicationIdAndScanId(em, appId, scanId);
    }
    finally {
      close(em);
    }
  }

  /**
   * Returns the most recent policy evaluation for the most recent scan for the given application and stage.
   */
  public PolicyEvaluation getLastByApplicationIdAndStageId(EntityManager em, String appId, String stageTypeId) {
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation per" + //
        " WHERE pe.id = per.policyEvaluationId" + //
        " AND per.applicationId=?1" + //
        " AND per.stageTypeId=?2";
    return get(em, sQuery, appId, stageTypeId);
  }

  public List<PolicyEvaluation> getLastByApplicationIdsAndStageIds(Set<String> appIds, Set<String> stageTypeIds) {
    String sQuery = "SELECT pe FROM PolicyEvaluation pe," + //
        " LastPolicyEvaluation per" + //
        " WHERE pe.id = per.policyEvaluationId" + //
        " AND per.applicationId in (?1)" + //
        " AND per.stageTypeId in (?2)";
    return getList(sQuery, appIds, stageTypeIds);
  }

  public PolicyEvaluation getLastByApplicationIdAndStageId(String appId, String stageTypeId) {
    EntityManager em = createEntityManager();
    try {
      return getLastByApplicationIdAndStageId(em, appId, stageTypeId);
    }
    finally {
      close(em);
    }
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given application and stage.
   */
  public PolicyEvaluation getLastPrimaryByApplicationIdAndStageId(EntityManager em, String appId, String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2 AND entity.isReevaluation=false" + //
        " ORDER BY entity.time DESC";
    return createQuery(sQuery, appId, stageTypeId).forceSingleResult().get(em);
  }

  /**
   * Returns the last primary evaluation (i.e. not a reevaluation) for the given application and stage.
   */
  public PolicyEvaluation getLastPrimaryByApplicationIdAndStageId(String appId, String stageTypeId) {
    EntityManager em = createEntityManager();
    try {
      return getLastPrimaryByApplicationIdAndStageId(em, appId, stageTypeId);
    }
    finally {
      close(em);
    }
  }

  public List<PolicyEvaluation> getAllByApplicationIdAndStageId(String appId, String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1 AND entity.stageTypeId=?2" + //
        " ORDER BY entity.time DESC";
    return getList(sQuery, appId, stageTypeId);
  }

  @Override
  public void insert(EntityManager em, PolicyEvaluation policyEvaluation) {
    validate(policyEvaluation);
    final LastPolicyEvaluationDAO lastPolicyEvaluationDAO = new LastPolicyEvaluationDAO();

    if (policyEvaluation.getTime() == null) {
      policyEvaluation.setTime(new Date());
    }
    super.insert(em, policyEvaluation);

    //make sure the last policy eval is right
    lastPolicyEvaluationDAO.deleteForApplicationIdAndStageTypeId(em, policyEvaluation.getApplicationId(),
        policyEvaluation.getStageTypeId());
    lastPolicyEvaluationDAO.insertIfPossibleLastPolicyEvaluation(em, policyEvaluation.getApplicationId(),
        policyEvaluation.getStageTypeId());
  }


  public List<PolicyEvaluation> getByApplicationId(EntityManager em, String appId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1";
    return getList(em, sQuery, appId);
  }

  public List<PolicyEvaluation> getByApplicationIdAndStageIds(String appId, Set<String> stageTypeIds) {
    EntityManager em = createEntityManager();
    try {
      return getByApplicationIdAndStageIds(em, appId, stageTypeIds);
    }
    finally {
      close(em);
    }
  }

  public List<PolicyEvaluation> getByApplicationIdAndStageIds(EntityManager em, String appId,
      Set<String> stageTypeIds)
  {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId = ?1 AND entity.stageTypeId IN (?2)" + //
        "   AND entity.isForObsoleteScan = false" + //
        " ORDER BY entity.time";
    return getList(em, sQuery, appId, stageTypeIds);
  }

  @Override
  public void update(EntityManager em, PolicyEvaluation entity) {
    throw new UnsupportedOperationException("The PolicyEvaluation table does not support update operations");
  }

  @Override
  public void delete(final EntityManager em, PolicyEvaluation entity) {
    final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    final LastPolicyEvaluationDAO lastPolicyEvaluationDAO = new LastPolicyEvaluationDAO();

    // Cascade to policy violations
    List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationId(em, entity.getId());
    for (PolicyViolation policyViolation : policyViolations) {
      policyViolationDAO.delete(em, policyViolation);
    }

    //cascade to LastPolicyEvaluation
    lastPolicyEvaluationDAO.deleteForApplicationIdAndStageTypeId(em, entity.getApplicationId(),
        entity.getStageTypeId());

    //delete the eval itself
    super.delete(em, entity);

    //insert if possible to LastPolicyEvaluation
    lastPolicyEvaluationDAO.insertIfPossibleLastPolicyEvaluation(em, entity.getApplicationId(),
        entity.getStageTypeId());
  }

  private void validate(PolicyEvaluation policyEvaluation) {
    if (!policyEvaluation.isReevaluation() && policyEvaluation.isForObsoleteScan()) {
      throw new IllegalStateException("Primary evaluations cannot be for obsolete scans");
    }
  }
}
