/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.AutoPolicyWaiver.AUTO_POLICY_WAIVER;

@Named
@Singleton
public class AutoPolicyWaiverDAO
    extends AbstractOperationalSqlDAO<AutoPolicyWaiver>
{
  private final AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  @Inject
  public AutoPolicyWaiverDAO(
      final OperationalDataStore operationalDataStore,
      final AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO)
  {
    super(operationalDataStore);
    this.autoPolicyWaiverExclusionDAO = autoPolicyWaiverExclusionDAO;
  }

  @Override
  public void delete(TransactionContext tx, AutoPolicyWaiver autoPolicyWaiver) {
    for (AutoPolicyWaiverExclusion autoPolicyWaiverExclusion : autoPolicyWaiverExclusionDAO
        .getByOwnerIdAndAutoPolicyWaiverId(autoPolicyWaiver.getOwnerId(), autoPolicyWaiver.getId()))
    {
      autoPolicyWaiverExclusionDAO.delete(autoPolicyWaiverExclusion);
    }
    tx.dsl()
        .deleteFrom(AUTO_POLICY_WAIVER)
        .where(AUTO_POLICY_WAIVER.AUTO_POLICY_WAIVER_ID.eq(autoPolicyWaiver.getId()))
        .execute();
  }

  public List<AutoPolicyWaiver> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<AutoPolicyWaiver> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(AUTO_POLICY_WAIVER)
        .where(AUTO_POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .fetch(this::toEntity);
  }

  public List<AutoPolicyWaiver> getByOwnerIds(Collection<String> ownerIds) {
    return getListWithSqlInClause(ownerIds,
        ids -> {
          try (TransactionContext tx = createTransactionContext()) {
            return tx.dsl()
                .selectFrom(AUTO_POLICY_WAIVER)
                .where(AUTO_POLICY_WAIVER.OWNER_ID.in(ids))
                .orderBy(AUTO_POLICY_WAIVER.THREAT_LEVEL.desc())
                .fetch(this::toEntity);
          }
        });
  }

  public AutoPolicyWaiver getByIdAndOwnerIdNotNull(String autoPolicyWaiverId, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdAndOwnerIdNotNull(tx, autoPolicyWaiverId, ownerId);
    }
  }

  public AutoPolicyWaiver getByIdAndOwnerIdNullable(String autoPolicyWaiverId, String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByIdAndOwnerId(tx, autoPolicyWaiverId, ownerId);
    }
  }

  public AutoPolicyWaiver getByIdAndOwnerIdNotNull(TransactionContext tx, String autoPolicyWaiverId, String ownerId) {
    AutoPolicyWaiver autoPolicyWaiver = getByIdAndOwnerId(tx, autoPolicyWaiverId, ownerId);
    if (autoPolicyWaiver == null) {
      String errorMessage = "Cannot find a waiver with ID " + autoPolicyWaiverId + " for owner " + ownerId + ".";
      throw new NotFoundException(errorMessage);
    }
    return autoPolicyWaiver;
  }

  public AutoPolicyWaiver getByIdAndOwnerId(TransactionContext tx, String autoPolicyWaiverId, String ownerId) {
    return toEntity(tx.dsl()
        .selectFrom(AUTO_POLICY_WAIVER)
        .where(AUTO_POLICY_WAIVER.AUTO_POLICY_WAIVER_ID.eq(autoPolicyWaiverId))
        .and(AUTO_POLICY_WAIVER.OWNER_ID.eq(ownerId))
        .fetchOne());
  }

  @Override
  public Table<?> getJooqTable() {
    return AUTO_POLICY_WAIVER;
  }

  @Override
  public Class<AutoPolicyWaiver> getEntityClass() {
    return AutoPolicyWaiver.class;
  }
}
