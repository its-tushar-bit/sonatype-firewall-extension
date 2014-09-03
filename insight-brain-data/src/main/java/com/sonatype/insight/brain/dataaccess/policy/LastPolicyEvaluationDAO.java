/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.policy.LastPolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.dataaccess.AbstractDAO;

/**
 * @since 1.12
 */
public class LastPolicyEvaluationDAO
    extends AbstractDAO<LastPolicyEvaluation>
{


  private EntityManagerFactory entityManagerFactory = OperationalDataStoreProvider.getJPAEntityManagerFactory();

  @Override
  public EntityManager createEntityManager() {
    return entityManagerFactory.createEntityManager();
  }

  public void insertIfPossibleLastPolicyEvaluation(final EntityManager em, final String applicationId,
      final String stageTypeId)
  {
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    //see if there is a most recent value
    String sQuery = "SELECT e from PolicyEvaluation e " + //
        "WHERE e.applicationId = ?1 " + //
        "AND e.stageTypeId = ?2 " + //
        "AND e.isForObsoleteScan = false " + //
        "ORDER BY e.time DESC";

    PolicyEvaluation newestPolicyEvaluation = policyEvaluationDAO.createQuery(sQuery, applicationId, stageTypeId)
        .forceSingleResult().get(em);
    if (newestPolicyEvaluation != null) {
      insert(em,
          new LastPolicyEvaluation(newestPolicyEvaluation.getId(), newestPolicyEvaluation.getApplicationId(),
              newestPolicyEvaluation.getStageTypeId()));
    }
  }

  public void deleteForApplicationIdAndStageTypeId(final EntityManager em, final String applicationId,
      final String stageTypeId)
  {
    LastPolicyEvaluation lpe = getByApplicationIdAndStageTypeId(em, applicationId, stageTypeId);
    if (lpe != null) {
      delete(em, lpe);
    }
  }

  public LastPolicyEvaluation getByEvaluationId(final EntityManager em, final String evaluationId) {
    String sQuery = "SELECT entity FROM LastPolicyEvaluation entity" + //
        " WHERE entity.policyEvaluationId=?1";
    return createQuery(sQuery, evaluationId).forceSingleResult().get(em);
  }

  public LastPolicyEvaluation getByEvaluationId(final String evaluationId) {
    EntityManager em = createEntityManager();
    try {
      return getByEvaluationId(em, evaluationId);
    }
    finally {
      close(em);
    }
  }

  public LastPolicyEvaluation getByApplicationIdAndStageTypeId(final EntityManager em, final String applicationId,
      final String stageTypeId)
  {
    String sQuery = "SELECT entity FROM LastPolicyEvaluation entity" + //
        " WHERE entity.applicationId = ?1" + //
        " AND entity.stageTypeId = ?2";
    return get(em, sQuery, applicationId, stageTypeId);
  }

  public LastPolicyEvaluation getByApplicationIdAndStageTypeId(final String applicationId,
      final String stageTypeId)
  {
    EntityManager em = createEntityManager();
    try {
      return getByApplicationIdAndStageTypeId(em, applicationId, stageTypeId);
    }
    finally {
      close(em);
    }
  }


  @Override
  public void update(EntityManager em, LastPolicyEvaluation entity) {
    throw new UnsupportedOperationException("The LastPolicyEvaluation table does not support update operations");
  }
}
