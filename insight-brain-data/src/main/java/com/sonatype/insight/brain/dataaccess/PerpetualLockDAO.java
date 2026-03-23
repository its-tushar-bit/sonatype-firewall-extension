/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PerpetualLock.PERPETUAL_LOCK;

@Named
@Singleton
public class PerpetualLockDAO
    extends AbstractOperationalSqlDAO<PerpetualLock>
{
  @Inject
  public PerpetualLockDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public PerpetualLock createPerpetualLock(
      final String perpetualLockId,
      final String category,
      final String owner,
      final Date expiration)
  {
    PerpetualLock perpetualLock = new PerpetualLock(category, perpetualLockId)
        .setOwner(owner)
        .setExpirationTime(expiration);
    insert(perpetualLock);
    return perpetualLock;
  }

  public List<PerpetualLock> getAllActivePartitionLocksForCategory(final String category) {
    Date currentTime = new Date();
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(PERPETUAL_LOCK)
          .where(PERPETUAL_LOCK.CATEGORY.eq(category))
          .and(PERPETUAL_LOCK.EXPIRATION_TIME.gt(currentTime))
          .orderBy(PERPETUAL_LOCK.EXPIRATION_TIME.asc())
          .fetch()
          .stream()
          .map(this::toEntity)
          .collect(Collectors.toList());
    }
  }

  public PerpetualLock getPerpetualLockById(final String perpetualLockId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(PERPETUAL_LOCK)
          .where(PERPETUAL_LOCK.PERPETUAL_LOCK_ID.eq(perpetualLockId))
          .fetchOne());
    }
  }

  public PerpetualLock getPerpetualLockByIdForUpdate(final TransactionContext txn, final String perpetualLockId) {
    return toEntity(txn.dsl()
        .selectFrom(PERPETUAL_LOCK)
        .where(PERPETUAL_LOCK.PERPETUAL_LOCK_ID.eq(perpetualLockId))
        .forUpdate()
        .fetchOne());
  }

  public int deleteExpiredLocks(final Date expirationCutoff) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int count = tx.dsl()
          .deleteFrom(PERPETUAL_LOCK)
          .where(PERPETUAL_LOCK.EXPIRATION_TIME.lt(expirationCutoff))
          .execute();
      tx.commit();
      return count;
    }
  }

  /**
   * Only the current owner of the lock can proactively release the lock
   *
   * @param perpetualLockId ID of perpetual lock to release
   * @param owner current owner of the lock (only the current owner would/should know this)
   */
  public void releasePerpetualLockForOwner(final String perpetualLockId, final String owner) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(PERPETUAL_LOCK)
          .setNull(PERPETUAL_LOCK.OWNER)
          .setNull(PERPETUAL_LOCK.EXPIRATION_TIME)
          .where(PERPETUAL_LOCK.PERPETUAL_LOCK_ID.eq(perpetualLockId))
          .and(PERPETUAL_LOCK.OWNER.eq(owner))
          .execute();
      tx.commit();
    }
  }

  public void releaseAllPerpetualLocksForOwner(final String owner) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(PERPETUAL_LOCK)
          .setNull(PERPETUAL_LOCK.OWNER)
          .setNull(PERPETUAL_LOCK.EXPIRATION_TIME)
          .where(PERPETUAL_LOCK.OWNER.eq(owner))
          .execute();
      tx.commit();
    }
  }

  public int reservePerpetualLock(
      final TransactionContext txn,
      final String perpetualLockId,
      final String owner,
      final Date expiration)
  {
    Date currentTime = new Date();
    return txn.dsl()
        .update(PERPETUAL_LOCK)
        .set(PERPETUAL_LOCK.OWNER, owner)
        .set(PERPETUAL_LOCK.EXPIRATION_TIME, expiration)
        .where(PERPETUAL_LOCK.PERPETUAL_LOCK_ID.eq(perpetualLockId))
        .and(PERPETUAL_LOCK.OWNER.eq(owner)
            .or(PERPETUAL_LOCK.OWNER.isNull())
            .or(PERPETUAL_LOCK.EXPIRATION_TIME.lt(currentTime)))
        .execute();
  }

  public int reservePerpetualLock(final String perpetualLockId, final String owner, final Date expiration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int result = reservePerpetualLock(tx, perpetualLockId, owner, expiration);
      tx.commit();
      return result;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return PERPETUAL_LOCK;
  }

  @Override
  public Class<PerpetualLock> getEntityClass() {
    return PerpetualLock.class;
  }
}
