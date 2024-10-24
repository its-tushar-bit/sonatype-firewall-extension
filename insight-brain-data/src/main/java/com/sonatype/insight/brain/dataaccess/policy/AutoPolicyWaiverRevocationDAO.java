/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.dataaccess.TransactionContext;

public class AutoPolicyWaiverRevocationDAO
    extends AbstractOperationalSqlDAO<AutoPolicyWaiverRevocation>
{
  @Inject
  public AutoPolicyWaiverRevocationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<AutoPolicyWaiverRevocation> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  public List<AutoPolicyWaiverRevocation> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM AutoPolicyWaiverRevocation entity" +
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<AutoPolicyWaiverRevocation> getByOwnerIdAndHash(String ownerId, String hash) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndHash(tx, ownerId, hash);
    }
  }

  public List<AutoPolicyWaiverRevocation> getByOwnerIdAndHash(TransactionContext tx, String ownerId, String hash) {
    String sQuery = "SELECT entity FROM AutoPolicyWaiverRevocation entity" +
        " WHERE entity.ownerId=?1 AND entity.hash=?2";
    return getList(tx, sQuery, ownerId, hash);
  }

  public List<AutoPolicyWaiverRevocation> getByOwnerIdAndAutoPolicyWaiverId(String ownerId, String autoPolicyWaiverId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndAutoPolicyWaiverId(tx, ownerId, autoPolicyWaiverId);
    }
  }

  public List<AutoPolicyWaiverRevocation> getByOwnerIdAndAutoPolicyWaiverId(
      TransactionContext tx,
      String ownerId,
      String autoPolicyWaiverId
  )
  {
    String sQuery = "SELECT entity FROM AutoPolicyWaiverRevocation entity" +
        " WHERE entity.ownerId=?1 AND entity.autoPolicyWaiverId=?2";
    return getList(tx, sQuery, ownerId, autoPolicyWaiverId);
  }

  public AutoPolicyWaiverRevocation getByOwnerIdAndAutoPolicyWaiverIdAndHash(
      String ownerId,
      String autoPolicyWaiverId,
      String hash
  )
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdAndAutoPolicyWaiverIdAndHash(tx, ownerId, autoPolicyWaiverId, hash);
    }
  }

  public AutoPolicyWaiverRevocation getByOwnerIdAndAutoPolicyWaiverIdAndHash(
      TransactionContext tx,
      String ownerId,
      String autoPolicyWaiverId,
      String hash
  )
  {
    String sQuery = "SELECT entity FROM AutoPolicyWaiverRevocation entity" +
        " WHERE entity.ownerId=?1 AND entity.autoPolicyWaiverId=?2 AND entity.hash=?3";
    return get(tx, sQuery, ownerId, autoPolicyWaiverId, hash);
  }
}
