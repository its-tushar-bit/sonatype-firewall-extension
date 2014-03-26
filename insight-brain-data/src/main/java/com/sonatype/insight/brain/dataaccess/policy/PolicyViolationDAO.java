/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

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
        " WHERE entity.policyEvaluationId=?1";
    return getList(em, sQuery, evaluationId);
  }

  public List<PolicyViolation> getByPolicyId(EntityManager em, String policyId) {
    String sQuery = "SELECT entity FROM PolicyViolation entity" + //
        " WHERE entity.policyId=?1";
    return getList(em, sQuery, policyId);
  }

  @Override
  public void update(EntityManager em, PolicyViolation entity) {
    throw new UnsupportedOperationException("The PolicyViolation table does not support update operations");
  }
}
