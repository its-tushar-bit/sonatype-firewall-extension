/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.exception.IntegrityConstraintViolationException;
import org.junit.Before;
import org.junit.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class PerpetualLockDAOTest
    extends AbstractDbDAOTest
{
  private static final String LOCK_CATEGORY = "testing";

  // test subject
  private PerpetualLockDAO perpetualLockDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    perpetualLockDAO = daoFactory.createPerpetualLockDAO();
    assertThat(perpetualLockDAO.getAll()).isEmpty();
  }

  @Test
  public void testCrud() {
    // setup:
    final String lockId = "test-lock-1";
    try (TransactionContext txn = perpetualLockDAO.createTransactionContext()) {
      txn.begin();
      // when: get non-existent lock
      PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockByIdForUpdate(txn, lockId);

      // then: lock doesn't exist
      assertThat(perpetualLock).isNull();
    }

    // when: create the lock
    Date expiration = new Date(currentTimeMillis() + 5_000);
    PerpetualLock perpetualLock = perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, "test-owner", expiration);

    // then: lock created
    assertThat(perpetualLock).isNotNull();
    PerpetualLock fetchedLock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(fetchedLock).isNotNull();
    assertThat(fetchedLock.getId()).isEqualTo(lockId);
    assertThat(fetchedLock.getOwner()).isEqualTo("test-owner");
    assertThat(fetchedLock.getExpirationTime()).isEqualTo(expiration);

    // when: reserve the lock (we already own it so it should be extended)
    Date previousExpiration = fetchedLock.getExpirationTime();
    expiration = new Date(currentTimeMillis() + 7_000);
    int reserveResult = perpetualLockDAO.reservePerpetualLock(lockId, "test-owner", expiration);

    // then: lock reservation extended
    assertThat(reserveResult).isEqualTo(1);
    PerpetualLock reservedLock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(reservedLock.getExpirationTime()).isAfter(previousExpiration);
  }

  @Test
  public void testCreatePerpetualLock_alreadyExists() {
    // given: existing perpetual lock
    final String lockId = "test-lock-2";
    perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, null, null);

    // when: try to create a duplicate lock
    Throwable throwable = catchThrowable(() -> perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY,
        "test-owner", new Date()));

    // then: an exception about the duplicate is raised (jOOQ throws IntegrityConstraintViolationException)
    assertThat(throwable).isInstanceOf(IntegrityConstraintViolationException.class);
  }

  @Test
  public void testReleasePerpetualLockForOwner_notForRequestedOwner() {
    // given: an existing perpetual lock assigned to an owner
    final String lockId = "test-lock-3";
    final Date expiration = new Date(currentTimeMillis() + 5_000);
    perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, "owner-1", expiration);

    // when: try to release the lock as a different owner
    perpetualLockDAO.releasePerpetualLockForOwner(lockId, "owner-2");

    // then: perpetual lock is not released
    PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(perpetualLock.getOwner()).isEqualTo("owner-1");
    assertThat(perpetualLock.getExpirationTime()).isEqualTo(expiration);
  }

  @Test
  public void testReleasePerpetualLockForOwner_sameOwner() {
    // given: an existing perpetual lock assigned to an owner
    final String lockId = "test-lock-4";
    final Date expiration = new Date(currentTimeMillis() + 5_000);
    perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, "owner-1", expiration);

    // when: try to release the lock as a different owner
    perpetualLockDAO.releasePerpetualLockForOwner(lockId, "owner-1");

    // then: perpetual lock is not released
    PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(perpetualLock.getOwner()).isNull();
    assertThat(perpetualLock.getExpirationTime()).isNull();
  }

  @Test
  public void testReleaseAllPerpetualLocksForOwner() {
    // given: a number of active locks for a given owner
    final Date expiration = new Date(currentTimeMillis() + 5_000);
    final String lockCategory = "owner-test";
    final String owner = "owner-1";
    perpetualLockDAO.createPerpetualLock("owner-1-lock-1", lockCategory, owner, expiration);
    perpetualLockDAO.createPerpetualLock("owner-1-lock-2", lockCategory, owner, expiration);
    perpetualLockDAO.createPerpetualLock("owner-1-lock-3", lockCategory, owner, expiration);
    perpetualLockDAO.createPerpetualLock("owner-1-lock-4", lockCategory, owner, expiration);
    assertThat(perpetualLockDAO.getAllActivePartitionLocksForCategory(lockCategory)).hasSize(4);

    // when:
    perpetualLockDAO.releaseAllPerpetualLocksForOwner(owner);

    // then: there are no longer any active locks for that owner
    assertThat(perpetualLockDAO.getAllActivePartitionLocksForCategory(lockCategory)).isEmpty();
  }

  @Test
  public void testReservePerpetualLock_differentOwner() {
    // given: an existing perpetual lock assigned to an owner
    final String lockId = "test-lock-5";
    final Date expiration = new Date(currentTimeMillis() + 5_000);
    perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, "owner-1", expiration);

    // then: unable to reserve lock for different user
    assertThat(perpetualLockDAO.reservePerpetualLock(lockId, "owner-2", new Date(currentTimeMillis() + 10_000)))
        .isZero();

    // and: original owner info did not change
    PerpetualLock reservedLock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(reservedLock.getOwner()).isEqualTo("owner-1");
    assertThat(reservedLock.getExpirationTime()).isEqualTo(expiration);
  }

  @Test
  public void testReservePerpetualLock_sameOwner() {
    // given: an existing perpetual lock assigned to an owner
    final String lockId = "test-lock-6";
    Date expiration = new Date(currentTimeMillis() + 5_000);
    perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, "owner-1", expiration);

    // when: lock reserved again for same user with new expiration time
    expiration = new Date(currentTimeMillis() + 10_000);

    // then: unable to reserve lock for different user
    assertThat(perpetualLockDAO.reservePerpetualLock(lockId, "owner-1", expiration))
        .isEqualTo(1);

    // and: original owner info did not change and reserve time updated
    PerpetualLock reservedLock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(reservedLock.getOwner()).isEqualTo("owner-1");
    assertThat(reservedLock.getExpirationTime()).isEqualTo(expiration);
  }

  @Test
  public void testReservePerpetualLock_differentOwnerAndExpired() {
    // given: an already expired perpetual lock assigned to an owner
    final String lockId = "test-lock-7";
    Date expiration = new Date(currentTimeMillis() - 5_000);
    perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, "owner-1", expiration);

    // when: lock reserved for different user
    expiration = new Date(currentTimeMillis() + 10_000);

    // then: unable to reserve lock for different user
    assertThat(perpetualLockDAO.reservePerpetualLock(lockId, "owner-2", expiration))
        .isEqualTo(1);

    // and: owner-2 now has the lock with its expiration time
    PerpetualLock reservedLock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(reservedLock.getOwner()).isEqualTo("owner-2");
    assertThat(reservedLock.getExpirationTime()).isEqualTo(expiration);
  }

  @Test
  public void testReservePerpetualLock_unassignedAndViaSelectForUpdate() {
    Date expiration = new Date(currentTimeMillis() + 3_000);
    // given: an existing unassigned perpetual lock
    final String lockId = "test-lock-8";
    perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, null, null);

    // when: select for update
    try (TransactionContext txn = perpetualLockDAO.createTransactionContext()) {
      txn.begin();
      PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockByIdForUpdate(txn, lockId);

      // then: lock selected
      assertThat(perpetualLock).isNotNull();

      // when: reserve lock
      assertThat(perpetualLockDAO.reservePerpetualLock(txn, lockId, "test-owner", expiration)).isEqualTo(1);
      txn.commit();
    }

    // then: lock was reserved
    PerpetualLock fetchedLock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(fetchedLock).isNotNull();
    assertThat(fetchedLock.getOwner()).isEqualTo("test-owner");
    assertThat(fetchedLock.getExpirationTime()).isEqualTo(expiration);
  }

  @Test
  public void testReservePerpetualLock_alreadyAssignedAndViaSelectForUpdate() {
    // given: an existing perpetual lock currently assigned
    final Date expiration1 = new Date(currentTimeMillis() + 3_000);
    final String lockId = "test-lock-9";
    perpetualLockDAO.createPerpetualLock(lockId, LOCK_CATEGORY, "test-owner-1", expiration1);

    // when: select for update
    try (TransactionContext txn = perpetualLockDAO.createTransactionContext()) {
      txn.begin();
      PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockByIdForUpdate(txn, lockId);

      // then: lock was selected
      assertThat(perpetualLock).isNotNull();

      // when: reserve lock
      Date expiration2 = new Date(currentTimeMillis() + 5_000);
      assertThat(perpetualLockDAO.reservePerpetualLock(txn, lockId, "test-owner-2", expiration2)).isZero();
      txn.commit();
    }

    // then: lock was NOT reserved
    PerpetualLock fetchedLock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(fetchedLock).isNotNull();
    assertThat(fetchedLock.getOwner()).isEqualTo("test-owner-1");
    assertThat(fetchedLock.getExpirationTime()).isEqualTo(expiration1);
  }
}
