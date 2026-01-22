/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

/**
 * @since 1.8
 */
@Named
@Singleton
public class PolicyMonitoringDAO
    extends AbstractOperationalSqlDAO<PolicyMonitoring>
{
  @Inject
  public PolicyMonitoringDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public List<PolicyMonitoring> getAll() {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " ORDER BY entity.id";
    return getList(sQuery);
  }

  public List<PolicyMonitoring> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public PolicyMonitoring getByOwnerIdAndStageTypeIdNotNull(String ownerId, String stageTypeId) {
    PolicyMonitoring entity = getByOwnerIdAndStageTypeId(ownerId, stageTypeId);
    if (entity == null) {
      throw new NotFoundException("Policy monitoring was not set for owner ID " + ownerId + ".");
    }
    return entity;
  }

  public List<PolicyMonitoring> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public PolicyMonitoring getByOwnerIdAndStageTypeId(String ownerId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndStageTypeId(tx, ownerId, stageTypeId);
    }
  }

  public PolicyMonitoring getByOwnerIdAndStageTypeId(TransactionContext tx, String ownerId, String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " WHERE entity.ownerId=?1 and entity.stageTypeId=?2";
    return get(tx, sQuery, ownerId, stageTypeId);
  }

  public List<PolicyMonitoring> getByStageTypeId(String stageTypeId) {
    String sQuery = "SELECT entity FROM PolicyMonitoring entity" + //
        " WHERE entity.stageTypeId=?1";
    return getList(sQuery, stageTypeId);
  }

  @Override
  /**
   * Inserts the policy monitoring for an app/org.
   *
   * This method enforces that there can be a maximum of 2 records per owner id
   * one for Lifecycle with any stage except compliance and another for SBOM Manager
   * with the compliance stage
   */
  public void insert(TransactionContext tx, PolicyMonitoring entity) {
    List<PolicyMonitoring> others = getByOwnerId(tx, entity.getOwnerId());
    if (others.stream().anyMatch(pM -> pM.getStageTypeId().equals(entity.getStageTypeId()) ||
        (!ComplianceStageType.ID.equals(others.get(0).getStageTypeId()) &&
            !ComplianceStageType.ID.equals(entity.getStageTypeId())))) {
      throw new BadRequestException("This application/organization already has policy monitoring.");
    }
    super.insert(tx, entity);
  }

  /**
   * Sets (insert or update) the policy monitoring for an app/org.
   *
   * This method enforces that there can be a maximum of 2 records per owner id
   * one for Lifecycle with any stage except compliance and another for SBOM Manager
   * with the compliance stage
   */
  public void set(PolicyMonitoring entity) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      PolicyMonitoring existing = null;
      if (ComplianceStageType.ID.equals(entity.getStageTypeId())) {
        existing = getByOwnerIdAndStageTypeId(tx, entity.getOwnerId(), ComplianceStageType.ID);
      }
      else {
        Optional<PolicyMonitoring> others = getByOwnerId(tx, entity.getOwnerId()).stream()
            .filter(pm -> !ComplianceStageType.ID.equals(pm.getStageTypeId())).findFirst();
        if (others.isPresent()) {
          existing = others.get();
        }
      }

      if (existing == null) {
        entity.setId(null);
        insert(tx, entity);
      }
      else {
        entity.setId(existing.getId());
        update(tx, entity);
      }
      tx.commit();
    }
  }
}
