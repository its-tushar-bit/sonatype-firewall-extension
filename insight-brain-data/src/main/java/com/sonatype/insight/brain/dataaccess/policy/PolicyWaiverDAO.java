/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

public class PolicyWaiverDAO
    extends AbstractOperationalSqlDAO<PolicyWaiver>
{
  @Override
  protected PolicyWaiver getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  private PolicyWaiver getByIdNotNull(EntityManager em, String id) {
    PolicyWaiver policyWaiver = getById(em, id);
    if (policyWaiver == null) {
      throw new NotFoundException("Cannot find a policy waiver with id " + id);
    }
    return policyWaiver;
  }

  public PolicyWaiver getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }

  public List<PolicyWaiver> getByOwnerId(String ownerId) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerId(em, ownerId);
    }
    finally {
      close(em);
    }
  }

  /**
   * Gets all policy waivers that target the specified component hash in the context of the given app/org. Note that a
   * component can be subject to a waiver that refers to its specific hash or to a waiver that applies to the entire
   * app/org.
   */
  public List<PolicyWaiver> getByOwnerIdAndHash(String ownerId, String hash) {
    EntityManager em = createEntityManager();
    try {
      List<PolicyWaiver> waivers = new ArrayList<PolicyWaiver>();
      waivers.addAll(getByOwnerIdAndHash(em, ownerId, hash));
      waivers.addAll(getByOwnerIdAndHash(em, ownerId, null));
      return waivers;
    }
    finally {
      close(em);
    }
  }

  public List<PolicyWaiver> getByOwnerId(String ownerId, boolean inherit) {
    EntityManager em = createEntityManager();
    try {
      List<PolicyWaiver> policyWaivers = new ArrayList<PolicyWaiver>();
      if (inherit) {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getById(ownerId);
        if (application != null) {
          policyWaivers.addAll(getByOwnerId(em, application.getOrganizationId()));
        }
      }
      policyWaivers.addAll(getByOwnerId(em, ownerId));
      return policyWaivers;
    }
    finally {
      close(em);
    }
  }

  private List<PolicyWaiver> getByOwnerIdAndHash(EntityManager em, String ownerId, String hash) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.ownerId=?1 AND entity.hash=?2";
    return getList(em, sQuery, ownerId, hash);
  }

  public List<PolicyWaiver> getByOwnerId(EntityManager em, String ownerId) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.ownerId=?1";
    return getList(em, sQuery, ownerId);
  }

  public List<PolicyWaiver> getByPolicyId(String policyId) {
    EntityManager em = createEntityManager();
    try {
      return getByPolicyId(em, policyId);
    }
    finally {
      close(em);
    }
  }

  public List<PolicyWaiver> getByPolicyId(EntityManager em, String policyId) {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.policyId=?1";
    return getList(em, sQuery, policyId);
  }

  private PolicyWaiver getByHashAndPolicyIdAndConstraintIdAndOwnerId(EntityManager em, String hash, String policyId,
      String constraintId, String ownerId)
  {
    String sQuery = "SELECT entity FROM PolicyWaiver entity" + //
        " WHERE entity.hash=?1 AND entity.policyId=?2 AND entity.constraintId=?3 AND entity.ownerId=?4";
    return get(em, sQuery, hash, policyId, constraintId, ownerId);
  }

  @Override
  public void insert(EntityManager em, PolicyWaiver entity) {
    PolicyWaiver other = getByHashAndPolicyIdAndConstraintIdAndOwnerId(em, entity.getHash(), entity.getPolicyId(),
        entity.getConstraintId(), entity.getOwnerId());
    if (other != null) {
      throw new BadRequestException("This policy waiver already exists");
    }
    if (entity.getComment() != null && entity.getComment().length() > 1000) {
      throw new BadRequestException("Comment length must not exceed 1000 characters");
    }

    entity.setCreateTime(new Date());

    super.insert(em, entity);
  }

  @Override
  public void update(EntityManager em, PolicyWaiver entity) {
    throw new UnsupportedOperationException();
  }
}
