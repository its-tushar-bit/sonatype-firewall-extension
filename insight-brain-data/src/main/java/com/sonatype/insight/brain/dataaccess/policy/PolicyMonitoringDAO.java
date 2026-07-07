/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyMonitoring.POLICY_MONITORING;

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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_MONITORING)
          .orderBy(POLICY_MONITORING.POLICY_MONITORING_ID)
          .fetchInto(PolicyMonitoring.class);
    }
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
    return tx.dsl()
        .selectFrom(POLICY_MONITORING)
        .where(POLICY_MONITORING.OWNER_ID.eq(ownerId))
        .fetchInto(PolicyMonitoring.class);
  }

  public List<PolicyMonitoring> getByOwnerIdWithHierarchy(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .select(POLICY_MONITORING.fields())
        .from(POLICY_MONITORING)
        .join(OWNER_ANCESTOR)
        .on(POLICY_MONITORING.OWNER_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
        .orderBy(OWNER_ANCESTOR.ANCESTOR_DISTANCE, POLICY_MONITORING.STAGE_TYPE_ID)
        .fetchInto(PolicyMonitoring.class);
  }

  public List<PolicyMonitoring> getByOwnerIdWithHierarchy(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdWithHierarchy(tx, ownerId);
    }
  }

  public PolicyMonitoring getByOwnerIdAndStageTypeId(String ownerId, String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndStageTypeId(tx, ownerId, stageTypeId);
    }
  }

  public PolicyMonitoring getByOwnerIdAndStageTypeId(TransactionContext tx, String ownerId, String stageTypeId) {
    return tx.dsl()
        .selectFrom(POLICY_MONITORING)
        .where(POLICY_MONITORING.OWNER_ID.eq(ownerId))
        .and(POLICY_MONITORING.STAGE_TYPE_ID.eq(stageTypeId))
        .fetchOneInto(PolicyMonitoring.class);
  }

  public List<PolicyMonitoring> getByStageTypeId(String stageTypeId) {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(POLICY_MONITORING)
          .where(POLICY_MONITORING.STAGE_TYPE_ID.eq(stageTypeId))
          .fetchInto(PolicyMonitoring.class);
    }
  }

  @Override
  /**
   * Inserts the policy monitoring for an app/org.
   *
   * This method enforces that there can be a maximum of 2 records per owner id
   * one for Lifecycle with any stage except compliance and another for SBOM Manager
   * with the compliance stage
   */
  public int insert(TransactionContext tx, PolicyMonitoring entity) {
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
    }
    List<PolicyMonitoring> others = getByOwnerId(tx, entity.getOwnerId());
    if (others.stream()
        .anyMatch(pM -> pM.getStageTypeId().equals(entity.getStageTypeId()) ||
            (!ComplianceStageType.ID.equals(others.get(0).getStageTypeId()) &&
                !ComplianceStageType.ID.equals(entity.getStageTypeId()))))
    {
      throw new BadRequestException("This application/organization already has policy monitoring.");
    }
    return tx.dsl()
        .insertInto(POLICY_MONITORING)
        .set(POLICY_MONITORING.POLICY_MONITORING_ID, entity.getId())
        .set(POLICY_MONITORING.OWNER_ID, entity.getOwnerId())
        .set(POLICY_MONITORING.STAGE_TYPE_ID, entity.getStageTypeId())
        .execute();
  }

  @Override
  public void delete(TransactionContext tx, PolicyMonitoring entity) {
    tx.dsl()
        .deleteFrom(POLICY_MONITORING)
        .where(POLICY_MONITORING.POLICY_MONITORING_ID.eq(entity.getId()))
        .execute();
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
            .filter(pm -> !ComplianceStageType.ID.equals(pm.getStageTypeId()))
            .findFirst();
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

  public Map<String, PolicyMonitoring> getByOwnerIdsAndStageTypeIdsWithInheritance(
      final Set<String> ownerIds,
      final String... stageTypeIds)
  {
    if (CollectionUtils.isEmpty(ownerIds)) {
      return new HashMap<>();
    }

    try (TransactionContext tx = createTransactionContext()) {
      List<org.jooq.Record> results = getListWithSqlInClause(ownerIds, partition -> {
        var query = tx.dsl()
            .select(OWNER_ANCESTOR.OWNER_ID, OWNER_ANCESTOR.ANCESTOR_DISTANCE)
            .select(POLICY_MONITORING.fields())
            .from(POLICY_MONITORING)
            .join(OWNER_ANCESTOR)
            .on(OWNER_ANCESTOR.ANCESTOR_ID.eq(POLICY_MONITORING.OWNER_ID))
            .where(OWNER_ANCESTOR.OWNER_ID.in(partition));

        if (ArrayUtils.isNotEmpty(stageTypeIds)) {
          query = query.and(POLICY_MONITORING.STAGE_TYPE_ID.in(stageTypeIds));
        }

        return query.fetch();
      });

      Map<String, PolicyMonitoring> map = new HashMap<>();
      Map<String, Integer> closestDistance = new HashMap<>();

      results.forEach(row -> {
        String ownerId = row.get(OWNER_ANCESTOR.OWNER_ID);
        int distance = row.get(OWNER_ANCESTOR.ANCESTOR_DISTANCE);

        if (!closestDistance.containsKey(ownerId) || distance < closestDistance.get(ownerId)) {
          closestDistance.put(ownerId, distance);
          map.put(ownerId, row.into(PolicyMonitoring.class));
        }
      });

      return map;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return POLICY_MONITORING;
  }

  @Override
  public Class<PolicyMonitoring> getEntityClass() {
    return PolicyMonitoring.class;
  }
}
