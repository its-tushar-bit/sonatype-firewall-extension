/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.9
 */
public class PolicyInternalDAO
    extends AbstractOperationalSqlDAO<PolicyInternal>
{
  @Override
  protected PolicyInternal getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  PolicyInternal getByIdNotNull(EntityManager em, String id) {
    PolicyInternal policyInternal = getById(em, id);
    if (policyInternal == null) {
      throw new NotFoundException("Cannot find a policy with id " + id);
    }
    return policyInternal;
  }

  PolicyInternal getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }

  List<PolicyInternal> getByOwnerIds(Collection<String> ownerIds) {
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.ownerId IN (?1)" + //
        " ORDER BY entity.nameLowercaseNoWhitespace";
    return getList(sQuery, ownerIds);
  }

  List<PolicyInternal> getByOwnerId(String ownerId) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerId(em, ownerId);
    }
    finally {
      close(em);
    }
  }

  List<PolicyInternal> getByOwnerId(EntityManager em, String ownerId) {
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.ownerId=?1" + //
        " ORDER BY entity.nameLowercaseNoWhitespace";
    return getList(em, sQuery, ownerId);
  }

  PolicyInternal getByOwnerIdAndName(EntityManager em, String ownerId, String name) {
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM PolicyInternal entity" + //
        " WHERE entity.ownerId=?1 AND entity.nameLowercaseNoWhitespace=?2";
    return get(em, sQuery, ownerId, name);
  }

  @Override
  public void delete(EntityManager em, PolicyInternal policy) {
    // Cascade to policy waivers
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    List<PolicyWaiver> policyWaivers = policyWaiverDAO.getByPolicyId(em, policy.getId());
    for (PolicyWaiver policyWaiver : policyWaivers) {
      policyWaiverDAO.delete(em, policyWaiver);
    }

    // Cascade to policy tags
    PolicyTagDAO policyTagDAO = new PolicyTagDAO();
    List<PolicyTag> policyTags = policyTagDAO.getByPolicyId(em, policy.getId());
    for (PolicyTag policyTag : policyTags) {
      policyTagDAO.delete(em, policyTag);
    }

    super.delete(em, policy);
  }
}
