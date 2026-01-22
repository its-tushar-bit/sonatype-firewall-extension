/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.concurrent;

import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.brain.utils.DateUtils;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.RollbackException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * A perpetual lock is a lock that 'expires' but can be perpetually 'renewed' by the current owner of the lock without
 * the need to keep a transaction open and tie up a DB connection
 */
@Named
@Singleton
public class PerpetualLockManager
{
  private static final Logger log = LoggerFactory.getLogger(PerpetualLockManager.class);

  private static final int EXPIRED_LOCK_CLEANUP_SECONDS = 60 * 60; // one hour

  private final PerpetualLockDAO perpetualLockDAO;

  @Inject
  public PerpetualLockManager(PerpetualLockDAO perpetualLockDAO) {
    this.perpetualLockDAO = perpetualLockDAO;
  }

  public void removeExpiredLocks() {
    Date expirationCutoff = new Date(System.currentTimeMillis() - EXPIRED_LOCK_CLEANUP_SECONDS * 1_000L);
    perpetualLockDAO.deleteExpiredLocks(expirationCutoff);
  }

  public List<PerpetualLock> getAllActivePerpetualLocksForCategory(String category) {
    return perpetualLockDAO.getAllActivePartitionLocksForCategory(category);
  }

  /**
   * Tries to acquire the given lock for the given owner. The lock will be granted to the requesting owner in the
   * following circumstances:
   * - The lock does not exist yet, in which case it will be created
   * - The lock is already owned by the requesting owner, in which case the expiration time will be updated
   * - The lock is owned by someone else but the expiration time has passed
   */
  public boolean tryAcquireLock(
      final String perpetualLockId,
      final String category,
      final String owner,
      final long expiresInXSeconds)
  {
    validateArgs(perpetualLockId, category, owner, expiresInXSeconds);
    boolean acquired = false;
    final String shortIdForLogging = shorten(perpetualLockId);
    final String shortOwnerForLogging = shorten(owner);
    final Date expiration = new Date(System.currentTimeMillis() + 1_000 * expiresInXSeconds);
    log.trace("Trying to acquire perpetual lock {} on behalf of {} to expire in {} seconds.",
        shortIdForLogging, shortOwnerForLogging, expiresInXSeconds);
    PerpetualLock perpetualLock = null;

    TransactionContext txn = null;

    try  {
      txn = perpetualLockDAO.createTransactionContext();
      txn.begin();
      perpetualLock = perpetualLockDAO.getPerpetualLockByIdForUpdate(txn, perpetualLockId);
      if (null != perpetualLock) {
        log.trace("Perpetual lock {} exists - owner: {}, exp. time: {}.", shortIdForLogging,
            shortOwnerForLogging, perpetualLock.getExpirationTime());
        acquired = reservePerpetualLock(txn, perpetualLockId, owner,
            DateUtils.max(perpetualLock.getExpirationTime(), expiration));
      }
      txn.commit();
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    finally {
      if (null != txn) {
        txn.close();
      }
    }

    if (null == perpetualLock) {
      acquired = createPerpetualLock(perpetualLockId, category, owner, expiration);
    }

    return acquired;
  }

  public void releasePerpetualLock(String perpetualLockId, String owner) {
    perpetualLockDAO.releasePerpetualLockForOwner(perpetualLockId, owner);
  }

  public void releasePerpetualLocksForOwner(String owner) {
    perpetualLockDAO.releaseAllPerpetualLocksForOwner(owner);
  }

  public void removePerpetualLock(String perpetualLockId) {
    PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockById(perpetualLockId);
    if (null != perpetualLock) {
      perpetualLockDAO.delete(perpetualLock);
    }
  }

  private boolean createPerpetualLock(String perpetualLockId, String category, String owner, Date expiration) {
    boolean acquired = false;
    final String shortIdForLogging = shorten(perpetualLockId);
    final String shortOwnerForLogging = shorten(owner);
    log.trace("Perpetual lock {} does not exist yet.  Creating...", shortIdForLogging);
    try {
      perpetualLockDAO.createPerpetualLock(perpetualLockId, category, owner, expiration);
      acquired = true;
      log.trace("Perpetual lock {} created and acquired on behalf of {}.", shortIdForLogging, shortOwnerForLogging);
    }
    catch (RollbackException e) {
      if (!(e.getCause() instanceof EntityExistsException)) {
        throw e;
      }
      // a simultaneous request to reserve this same lock beat us to it;  may have come from a different
      // instance or a different caller in this same instance, so we'll check to see if we can reserve it
      // instead of just assuming we can't
      log.trace("Perpetual lock {} already exists.  Will try to reserve it now on behalf of {}.",
          shortIdForLogging, shortOwnerForLogging);
      acquired = reservePerpetualLock(null, perpetualLockId, owner, expiration);
    }
    return acquired;
  }

  private boolean reservePerpetualLock(TransactionContext txn, String perpetualLockId, String owner, Date expiration) {
    boolean acquired = false;
    if (null != txn) {
      acquired = 1 == perpetualLockDAO.reservePerpetualLock(txn, perpetualLockId, owner, expiration);
    }
    else {
      acquired = 1 == perpetualLockDAO.reservePerpetualLock(perpetualLockId, owner, expiration);
    }
    log.trace("Perpetual lock {} on behalf of {} {} acquired.", shorten(perpetualLockId), shorten(owner),
        acquired ? "was" : "was NOT");
    return acquired;
  }

  private String shorten(String source) {
    return null != source ? source.substring(0, 5) : null;
  }

  private void validateArgs(String perpetualLockId, String category, String owner, long expiresInXSeconds) {
    if (isBlank(perpetualLockId)) {
      throw new IllegalArgumentException("Required perpetualLockId is blank or missing.");
    }
    if (isBlank(category)) {
      throw new IllegalArgumentException("Required perpetual lock category is blank or missing.");
    }
    if (isBlank(owner)) {
      throw new IllegalArgumentException("Required perpetual lock owner is blank or missing.");
    }
    if (expiresInXSeconds <= 0) {
      throw new IllegalArgumentException(
          format("Perpetual lock expiration of %d seconds is invalid.", expiresInXSeconds));
    }
  }
}
