/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.dataaccess.TransactionContext;

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
    String sQuery = "SELECT entity FROM AutoPolicyWaiverExclusion entity" +
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerIdAndHash(String ownerId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndHash(tx, ownerId, hash);
    }
  }

  public List<AutoPolicyWaiverExclusion> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    String sQuery = "SELECT entity FROM AutoPolicyWaiverExclusion entity" +
        " WHERE entity.ownerId=?1 AND entity.hash=?2";
    return getList(tx, sQuery, ownerId, hash);
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
    String sQuery = "SELECT entity FROM AutoPolicyWaiverExclusion entity" +
        " WHERE entity.ownerId=?1 AND entity.autoPolicyWaiverId=?2";
    return getList(tx, sQuery, ownerId, autoPolicyWaiverId);
  }

  public AutoPolicyWaiverExclusion getByOwnerIdAndAutoPolicyWaiverIdAndHash(
      String ownerId,
      String autoPolicyWaiverId,
      String hash
  )
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndAutoPolicyWaiverIdAndHash(tx, ownerId, autoPolicyWaiverId, hash);
    }
  }

  public AutoPolicyWaiverExclusion getByOwnerIdAndAutoPolicyWaiverIdAndHash(
      TransactionContext tx,
      String ownerId,
      String autoPolicyWaiverId,
      String hash
  )
  {
    String sQuery = "SELECT entity FROM AutoPolicyWaiverExclusion entity" +
        " WHERE entity.ownerId=?1 AND entity.autoPolicyWaiverId=?2 AND entity.hash=?3";
    return get(tx, sQuery, ownerId, autoPolicyWaiverId, hash);
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
    String sQuery = "SELECT entity FROM AutoPolicyWaiverExclusion entity" +
        " WHERE entity.ownerId=?1 AND entity.autoPolicyWaiverId=?2 AND entity.policyViolationId=?3";
    return get(tx, sQuery, ownerId, autoPolicyWaiverId, policyViolationId);
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

  @SuppressWarnings("unchecked")
  public List<AutoPolicyWaiverExclusion> getByOwnerIdAndAutoPolicyWaiverIdPaginated(
      TransactionContext tx,
      String ownerId,
      String autoPolicyWaiverId,
      int page,
      int pageSize)
  {
    jakarta.persistence.Query paginatedQuery = createPaginatedGetByOwnerIdAndAutoPolicyWaiverIdQuery(
        pageSize,
        page,
        ownerId,
        autoPolicyWaiverId,
        tx);
    return paginatedQuery.getResultStream().toList();
  }

  private jakarta.persistence.Query createPaginatedGetByOwnerIdAndAutoPolicyWaiverIdQuery(
      final int pageSize,
      final int page,
      final String ownerId,
      final String autoPolicyWaiverId,
      final TransactionContext tx)
  {
    int offset = (page - 1) * pageSize;
    String sQuery = "SELECT entity FROM AutoPolicyWaiverExclusion entity" +
        " WHERE entity.ownerId=?1 AND entity.autoPolicyWaiverId=?2";
    jakarta.persistence.Query paginatedQuery = createPaginationQuery(tx, sQuery, offset, pageSize);
    paginatedQuery.setParameter(1, ownerId);
    paginatedQuery.setParameter(2, autoPolicyWaiverId);
    return paginatedQuery;
  }
}
