/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.7.1
 */
public class PolicyMonitoringDAO
    extends AbstractOperationalSqlDAO<PolicyMonitoring>
{
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
}
