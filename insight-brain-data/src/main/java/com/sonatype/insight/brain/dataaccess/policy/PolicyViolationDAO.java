/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.joda.time.DateTime;

/**
 * @since 1.11
 */
public class PolicyViolationDAO
    extends AbstractOperationalSqlDAO<PolicyViolation>
{
  @Override
  protected PolicyViolation getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public List<PolicyViolation> getByEvaluationId(String evaluationId) {
    EntityManager em = createEntityManager();
    try {
      return getByEvaluationId(em, evaluationId);
    }
    finally {
      close(em);
    }
  }

  public List<PolicyViolation> getByEvaluationId(EntityManager em, String evaluationId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyEvaluationId=?1" + //
        " ORDER BY entity.policyId, entity.groupId, entity.artifactId, entity.version, entity.hash";
    return getList(em, sQuery, evaluationId);
  }

  public List<PolicyViolation> getByEvaluationIds(Set<String> evaluationIds) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyEvaluationId IN (?1)";
    return getList(sQuery, evaluationIds);
  }

  public List<PolicyViolation> getByPolicyId(EntityManager em, String policyId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyId=?1";
    return getList(em, sQuery, policyId);
  }

  public List<PolicyViolation> getNewestByApplicationId(EntityManager em, String appId) {
    String sQuery = "SELECT policyViolation" + //
        " FROM PolicyViolation policyViolation, NewestPolicyViolation newestPolicyViolation" + //
        " WHERE policyViolation.id=newestPolicyViolation.id AND newestPolicyViolation.applicationId=?1" + //
        " ORDER BY policyViolation.policyId, policyViolation.groupId, policyViolation.artifactId, policyViolation.version, policyViolation.hash";
    return getList(em, sQuery, appId);
  }

  public List<PolicyViolation> getNewestByApplicationId(String appId) {
    EntityManager em = createEntityManager();
    try {
      return getNewestByApplicationId(em, appId);
    }
    finally {
      close(em);
    }
  }

  public List<PolicyViolation> getNewestByApplicationIdAndStageTypeIdAndLastNDays(String appId, String stageTypeId,
      int lastNDays)
  {
    String sQuery = "SELECT policyViolation" + //
        " FROM PolicyViolation policyViolation, NewestPolicyViolation newestPolicyViolation" + //
        " WHERE policyViolation.id=newestPolicyViolation.id AND newestPolicyViolation.applicationId=?1" + //
        " AND newestPolicyViolation.stageTypeId=?2" + //
        " AND policyViolation.time>?3" + //
        " ORDER BY policyViolation.policyId, policyViolation.groupId, policyViolation.artifactId, policyViolation.version, policyViolation.hash";
    DateTime now = new DateTime();
    return getList(sQuery, appId, stageTypeId, now.minusDays(lastNDays).toDate());
  }

  @Override
  public void delete(EntityManager em, PolicyViolation entity) {
    // Cascade to newest policy violation
    NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();
    NewestPolicyViolation newestPolicyViolation = newestPolicyViolationDAO.getById(em, entity.getId());
    if (newestPolicyViolation != null) {
      newestPolicyViolationDAO.delete(em, newestPolicyViolation);
    }

    super.delete(em, entity);
  }

  public List<PolicyViolation> getByEvaluationIdAndHash(String evaluationId, String hash) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyEvaluationId=?1 AND entity.hash=?2" + //
        " ORDER BY entity.policyId";
    return getList(sQuery, evaluationId, hash);
  }
}
