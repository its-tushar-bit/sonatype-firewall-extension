/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.8
 */
public class PolicyMonitoringDAO
    extends AbstractOperationalSqlDAO<PolicyMonitoring>
{
  @Override
  public List<PolicyMonitoring> getAll() {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " ORDER BY entity.id";
    return getList(sQuery);
  }

  public PolicyMonitoring getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public PolicyMonitoring getByOwnerIdNotNull(String ownerId) {
    PolicyMonitoring entity = getByOwnerId(ownerId);
    if (entity == null) {
      throw new NotFoundException("Policy monitoring was not set for owner ID " + ownerId + ".");
    }
    return entity;
  }

  public PolicyMonitoring getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " WHERE entity.ownerId=?1";
    return get(tx, sQuery, ownerId);
  }

  public List<PolicyMonitoring> getByStageTypeId(String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " WHERE entity.stageTypeId=?1";
    return getList(sQuery, stageTypeId);
  }

  @Override
  public void insert(TransactionContext tx, PolicyMonitoring entity) {
    PolicyMonitoring other = getByOwnerId(tx, entity.getOwnerId());
    if (other != null) {
      throw new BadRequestException("This application/organization already has policy monitoring.");
    }

    super.insert(tx, entity);
  }

  /**
   * Sets (insert or update) the policy monitoring for an app/org.
   */
  public void set(PolicyMonitoring entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      PolicyMonitoring other = getByOwnerId(tx, entity.getOwnerId());
      if (other == null) {
        entity.setId(null);
        insert(tx, entity);
      }
      else {
        entity.setId(other.getId());
        update(tx, entity);
      }
      tx.commit();
    }
  }
}
