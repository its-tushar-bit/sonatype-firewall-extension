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
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;

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

  public List<PolicyViolation> getFirstOccurrenceByApplicationIdAndStageTypeId(EntityManager em, String appId,
      String stageTypeId)
  {
    String sQuery = "SELECT policyViolation" + //
        " FROM PolicyViolation policyViolation," + //
        "   FirstOccurrencePolicyViolation firstOccurrencePolicyViolation" + //
        " WHERE policyViolation.id=firstOccurrencePolicyViolation.id" + //
        "   AND firstOccurrencePolicyViolation.applicationId=?1" + //
        "   AND firstOccurrencePolicyViolation.stageTypeId=?2" + //
        " ORDER BY policyViolation.policyId, policyViolation.groupId, policyViolation.artifactId, policyViolation.version, policyViolation.hash";
    return getList(em, sQuery, appId, stageTypeId);
  }

  public List<PolicyViolation> getFirstOccurrenceByApplicationIdAndStageTypeId(String appId, String stageTypeId) {
    EntityManager em = createEntityManager();
    try {
      return getFirstOccurrenceByApplicationIdAndStageTypeId(em, appId, stageTypeId);
    }
    finally {
      close(em);
    }
  }

  @Override
  public void delete(EntityManager em, PolicyViolation entity) {
    // Cascade to first occurrence policy violation
    FirstOccurrencePolicyViolationDAO firstOccurrencePolicyViolationDAO = new FirstOccurrencePolicyViolationDAO();
    FirstOccurrencePolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationDAO.getById(em,
        entity.getId());
    if (firstOccurrencePolicyViolation != null) {
      firstOccurrencePolicyViolationDAO.delete(em, firstOccurrencePolicyViolation);
    }

    // Cascade to waived policy violation
    if (entity.isWaived()) {
      WaivedPolicyViolationDAO waivedPolicyViolationDAO = new WaivedPolicyViolationDAO();
      WaivedPolicyViolation waivedPolicyViolation = waivedPolicyViolationDAO.getById(em, entity.getId());
      if (waivedPolicyViolation != null) {
        waivedPolicyViolationDAO.delete(em, waivedPolicyViolation);
      }
    }

    super.delete(em, entity);
  }

  public List<PolicyViolation> getByEvaluationIdAndHash(String evaluationId, String hash) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyEvaluationId=?1 AND entity.hash=?2" + //
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
