/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.AutoPolicyWaiverRevocation.AUTO_POLICY_WAIVER_REVOCATION;

@Named
@Singleton
public class AutoPolicyWaiverExclusionDAO
    extends AbstractOperationalSqlDAO<AutoPolicyWaiverExclusion>
{
  PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO;

  @Inject
  public AutoPolicyWaiverExclusionDAO(
      final OperationalDataStore operationalDataStore,
      final PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO)
  {
    super(operationalDataStore);
    this.policyViolationConstraintFactsDAO = policyViolationConstraintFactsDAO;
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerId(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .selectFrom(AUTO_POLICY_WAIVER_REVOCATION)
        .where(AUTO_POLICY_WAIVER_REVOCATION.OWNER_ID.eq(ownerId))
        .fetch(this::toEntity);
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerIdAndHash(String ownerId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndHash(tx, ownerId, hash);
    }
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    return tx.dsl()
        .selectFrom(AUTO_POLICY_WAIVER_REVOCATION)
        .where(AUTO_POLICY_WAIVER_REVOCATION.OWNER_ID.eq(ownerId))
        .and(AUTO_POLICY_WAIVER_REVOCATION.HASH.eq(hash))
        .fetch(this::toEntity);
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerIdAndAutoPolicyWaiverId(
      String ownerId,
      String autoPolicyWaiverId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndAutoPolicyWaiverId(tx, ownerId, autoPolicyWaiverId);
    }
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerIdAndAutoPolicyWaiverId(
      TransactionContext tx,
      String ownerId,
      String autoPolicyWaiverId)
  {
    return tx.dsl()
        .selectFrom(AUTO_POLICY_WAIVER_REVOCATION)
        .where(AUTO_POLICY_WAIVER_REVOCATION.OWNER_ID.eq(ownerId))
        .and(AUTO_POLICY_WAIVER_REVOCATION.AUTO_POLICY_WAIVER_ID.eq(autoPolicyWaiverId))
        .fetch(this::toEntity);
  }

  public AutoPolicyWaiverExclusion getByOwnerIdAndAutoPolicyWaiverIdAndHash(
      String ownerId,
      String autoPolicyWaiverId,
      String hash)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndAutoPolicyWaiverIdAndHash(tx, ownerId, autoPolicyWaiverId, hash);
    }
  }

  public AutoPolicyWaiverExclusion getByOwnerIdAndAutoPolicyWaiverIdAndHash(
      TransactionContext tx,
      String ownerId,
      String autoPolicyWaiverId,
      String hash)
  {
    return toEntity(tx.dsl()
        .selectFrom(AUTO_POLICY_WAIVER_REVOCATION)
        .where(AUTO_POLICY_WAIVER_REVOCATION.OWNER_ID.eq(ownerId))
        .and(AUTO_POLICY_WAIVER_REVOCATION.AUTO_POLICY_WAIVER_ID.eq(autoPolicyWaiverId))
        .and(AUTO_POLICY_WAIVER_REVOCATION.HASH.eq(hash))
        .fetchOne());
  }

  public AutoPolicyWaiverExclusion getByOwnerIdPolicyViolation(
      String ownerId,
      String autoPolicyWaiverId,
      String policyViolationId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdPolicyViolation(tx, ownerId, autoPolicyWaiverId, policyViolationId);
    }
  }

  public AutoPolicyWaiverExclusion getByOwnerIdPolicyViolation(
      TransactionContext tx,
      String ownerId,
      String autoPolicyWaiverId,
      String policyViolationId)
  {
    return toEntity(tx.dsl()
        .selectFrom(AUTO_POLICY_WAIVER_REVOCATION)
        .where(AUTO_POLICY_WAIVER_REVOCATION.OWNER_ID.eq(ownerId))
        .and(AUTO_POLICY_WAIVER_REVOCATION.AUTO_POLICY_WAIVER_ID.eq(autoPolicyWaiverId))
        .and(AUTO_POLICY_WAIVER_REVOCATION.POLICY_VIOLATION_ID.eq(policyViolationId))
        .fetchOne());
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerIdAndAutoPolicyWaiverIdPaginated(
      String ownerId,
      String autoPolicyWaiverId,
      int page,
      int pageSize)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndAutoPolicyWaiverIdPaginated(tx, ownerId, autoPolicyWaiverId, page, pageSize);
    }
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerIdAndAutoPolicyWaiverIdPaginated(
      TransactionContext tx,
      String ownerId,
      String autoPolicyWaiverId,
      int page,
      int pageSize)
  {
    int offset = (page - 1) * pageSize;
    return tx.dsl()
        .selectFrom(AUTO_POLICY_WAIVER_REVOCATION)
        .where(AUTO_POLICY_WAIVER_REVOCATION.OWNER_ID.eq(ownerId))
        .and(AUTO_POLICY_WAIVER_REVOCATION.AUTO_POLICY_WAIVER_ID.eq(autoPolicyWaiverId))
        .offset(offset)
        .limit(pageSize)
        .fetch(this::toEntity);
  }

  @Override
  public Table<?> getJooqTable() {
    return AUTO_POLICY_WAIVER_REVOCATION;
  }

  @Override
  public Class<AutoPolicyWaiverExclusion> getEntityClass() {
    return AutoPolicyWaiverExclusion.class;
  }
}
