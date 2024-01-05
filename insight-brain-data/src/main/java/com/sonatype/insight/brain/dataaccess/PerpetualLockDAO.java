/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.LockModeType;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class PerpetualLockDAO
    extends AbstractOperationalSqlDAO<PerpetualLock>
{
  private static final String RESERVE_QUERY =
      "UPDATE PerpetualLock entity SET entity.owner = ?2, entity.expirationTime = ?3 " +
      " WHERE entity.id = ?1 AND (entity.owner = ?2 OR entity.owner IS NULL OR entity.expirationTime < ?4)";

  @Inject
  public PerpetualLockDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public PerpetualLock createPerpetualLock(String perpetualLockId, String owner, Date expiration) {
    PerpetualLock perpetualLock = new PerpetualLock(perpetualLockId)
        .setOwner(owner)
        .setExpirationTime(expiration);
    insert(perpetualLock);
    return perpetualLock;
  }

  public PerpetualLock getPerpetualLockById(String perpetualLockId) {
    Query<PerpetualLock> sQuery = createQuery(
        "SELECT entity FROM PerpetualLock entity WHERE entity.id = ?1", perpetualLockId);

    return sQuery.get();
  }

  public PerpetualLock getPerpetualLockByIdForUpdate(TransactionContext txn, String perpetualLockId) {
    Query<PerpetualLock> sQuery = createQuery(
        "SELECT entity FROM PerpetualLock entity WHERE entity.id = ?1", perpetualLockId)
        .setLockModeType(LockModeType.PESSIMISTIC_WRITE);

    return sQuery.get(txn);
  }

  /**
   * Only the current owner of the lock can proactively release the lock
   *
   * @param perpetualLockId ID of perpetual lock to release
   * @param owner           current owner of the lock (only the current owner would/should know this)
   */
  public void releasePerpetualLockForOwner(String perpetualLockId, String owner) {
    final String sQuery = "UPDATE PerpetualLock entity SET entity.owner = null, entity.expirationTime = null" +
        " WHERE entity.id = ?1 AND entity.owner = ?2";
    createQuery(sQuery, perpetualLockId, owner).executeUpdate();
  }

  public int reservePerpetualLock(TransactionContext txn, String perpetualLockId, String owner, Date expiration) {
    Date currentTime = new Date();
    return createQuery(RESERVE_QUERY, perpetualLockId, owner, expiration, currentTime).executeUpdate(txn);
  }

  public int reservePerpetualLock(String perpetualLockId, String owner, Date expiration) {
    Date currentTime = new Date();
    return createQuery(RESERVE_QUERY, perpetualLockId, owner, expiration, currentTime).executeUpdate();
  }
}
