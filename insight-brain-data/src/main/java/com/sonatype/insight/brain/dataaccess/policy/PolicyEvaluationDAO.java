/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
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
    // This can be implemented simpler in two steps, by getting the last scan for the specified app and stage (by
    // getting the last primary evaluation) and then getting the last evaluation for that scan.
    // We chose to implement it as a single query in order to have one round trip to the db instead of two.
    String sQuery = "SELECT pe1 FROM PolicyEvaluation pe1" + //
        " WHERE pe1.applicationId=?1 AND pe1.stageTypeId=?2" + //
        "   AND pe1.time=(" + //
        // Find the time for the most recent evaluation
        "   SELECT max(pe2.time) FROM PolicyEvaluation pe2" + //
        "     WHERE pe2.applicationId=?1 AND pe2.stageTypeId=?2" + //
        "       AND pe2.isForObsoleteScan=false" + //
        "   )";
    return get(em, sQuery, appId, stageTypeId);
  }

  /**
   * Returns the most recent policy evaluation before a specified date for the most recent scan for the given
   * application and stage.
   */
  public PolicyEvaluation getLastBeforeDateByApplicationIdAndStageId(String appId, String stageTypeId, Date date) {
    // This can be implemented simpler in two steps, by getting the last scan for the specified app and stage (by
    // getting the last primary evaluation) and then getting the last evaluation for that scan.
    // We chose to implement it as a single query in order to have one round trip to the db instead of two.
    String sQuery = "SELECT pe1 FROM PolicyEvaluation pe1" + //
        " WHERE pe1.applicationId=?1 AND pe1.stageTypeId=?2" + //
        "   AND pe1.time=(" + //
        // Find the time for the most recent evaluation before the specified date
        "   SELECT max(pe2.time) FROM PolicyEvaluation pe2" + //
        "     WHERE pe2.applicationId=?1 AND pe2.stageTypeId=?2" + //
        "       AND pe2.isForObsoleteScan=false" + //
        "       AND pe2.time <= ?3" + //
        "   )";
    return get(sQuery, appId, stageTypeId, date);
  }

  //the fancy query for this is slower than the query above, so simple it is
  public List<PolicyEvaluation> getLastByApplicationIdsAndStageIds(Set<String> appIds, Set<String> stageTypeIds) {
    List<PolicyEvaluation> result = new ArrayList<>(appIds.size() * stageTypeIds.size());
    for (String stageTypeId : stageTypeIds) {
      for (String appId : appIds) {
        PolicyEvaluation eval = getLastByApplicationIdAndStageId(appId, stageTypeId);
        if (eval != null) {
          //can get null due to the code above :/
          result.add(eval);
        }
      }
    }
    return result;
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

    if (policyEvaluation.getTime() == null) {
      policyEvaluation.setTime(new Date());
    }
    super.insert(em, policyEvaluation);
  }

  public List<PolicyEvaluation> getByApplicationId(EntityManager em, String appId) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId=?1";
    return getList(em, sQuery, appId);
  }

  public List<PolicyEvaluation> getBetweenDatesByApplicationIdAndStageIds(String appId, Set<String> stageTypeIds, Date fromDate, Date toDate) {
    String sQuery = "SELECT entity FROM PolicyEvaluation entity" + //
        " WHERE entity.applicationId = ?1 AND entity.stageTypeId IN (?2)" + //
        "   AND entity.isForObsoleteScan=false" + //
        "   AND entity.time > ?3 AND entity.time <= ?4" + //
        " ORDER BY entity.time";
    return getList(sQuery, appId, stageTypeIds, fromDate, toDate);
  }

  @Override
  public void update(EntityManager em, PolicyEvaluation entity) {
    throw new UnsupportedOperationException("The PolicyEvaluation table does not support update operations");
  }

  @Override
  public void delete(EntityManager em, PolicyEvaluation entity) {
    // Cascade to policy violations
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    List<PolicyViolation> policyViolations = policyViolationDAO.getByEvaluationId(em, entity.getId());
    for (PolicyViolation policyViolation : policyViolations) {
      policyViolationDAO.delete(em, policyViolation);
    }

    super.delete(em, entity);
  }

  private void validate(PolicyEvaluation policyEvaluation) {
    if (!policyEvaluation.isReevaluation() && policyEvaluation.isForObsoleteScan()) {
      throw new IllegalStateException("Primary evaluations cannot be for obsolete scans");
    }
  }
}
