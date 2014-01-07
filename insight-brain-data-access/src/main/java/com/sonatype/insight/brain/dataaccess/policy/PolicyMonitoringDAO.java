/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.8
 */
public class PolicyMonitoringDAO
    extends AbstractOperationalSqlDAO<PolicyMonitoring>
{
  public List<PolicyMonitoring> getAll() {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " ORDER BY entity.id";
    return getList(sQuery);
  }

  @Override
  protected PolicyMonitoring getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public PolicyMonitoring getByOwnerId(String ownerId) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerId(em, ownerId);
    }
    finally {
      close(em);
    }
  }

  public PolicyMonitoring getByOwnerIdNotNull(String ownerId) {
    PolicyMonitoring entity = getByOwnerId(ownerId);
    if (entity == null) {
      throw new NotFoundException("Policy monitoring was not set for owner id " + ownerId);
    }
    return entity;
  }

  public PolicyMonitoring getByOwnerId(EntityManager em, String ownerId) {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " WHERE entity.ownerId=?1";
    return get(em, sQuery, ownerId);
  }

  @Override
  public void insert(EntityManager em, PolicyMonitoring entity) {
    PolicyMonitoring other = getByOwnerId(em, entity.getOwnerId());
    if (other != null) {
      throw new BadRequestException("This application/organization already has policy monitoring.");
    }

    super.insert(em, entity);
  }

  /**
   * Sets (insert or update) the policy monitoring for an app/org.
   */
  public void set(PolicyMonitoring entity) {
    EntityManager em = createEntityManager();
    try {
      em.getTransaction().begin();
      PolicyMonitoring other = getByOwnerId(em, entity.getOwnerId());
      if (other == null) {
        entity.setId(null);
        insert(em, entity);
      }
      else {
        entity.setId(other.getId());
        update(em, entity);
      }
      em.getTransaction().commit();
    }
    finally {
      close(em);
    }
  }
}
