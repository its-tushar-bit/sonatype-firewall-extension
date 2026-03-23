/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.concurrent;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import org.jooq.exception.DataAccessException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@Category(SlowTest.class)
public class PerpetualLockManagerTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(PerpetualLockManager.class);

  @Inject
  private PerpetualLockDAO perpetualLockDAO;

  // subject
  private PerpetualLockManager perpetualLockManager;

  @Before
  public void before() {
    logOutput.setLogLevel(Level.TRACE);

    perpetualLockManager = new PerpetualLockManager(perpetualLockDAO);
    assertThat(perpetualLockDAO.getAll()).hasSize(0);
  }

  @Test
  public void testTryAcquirePerpetualLock_invalidArgs() {
    // when: id is blank
    assertIllegalArgumentExceptionThrownOnTryAcquireLock(" ", "cat", "owner", 1,
        "Required perpetualLockId is blank or missing.");

    // when: id is null
    assertIllegalArgumentExceptionThrownOnTryAcquireLock(null, "cat", "owner", 1,
        "Required perpetualLockId is blank or missing.");

    // when: owner is blank
    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", "cat", " ", 1,
        "Required perpetual lock owner is blank or missing.");

    // when: owner is null
    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", "cat", null, 1,
        "Required perpetual lock owner is blank or missing.");

    // when: expires is 0
    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", "cat", "owner", 0,
        "Perpetual lock expiration of 0 seconds is invalid.");

    // when: expires is negative
    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", "cat", "owner", -1,
        "Perpetual lock expiration of -1 seconds is invalid.");
  }

  private void assertIllegalArgumentExceptionThrownOnTryAcquireLock(
      String perpetualLockId,
      String category,
      String ownerId,
      long expiration,
      String expectedMessage)
  {
    assertThatThrownBy(() -> perpetualLockManager.tryAcquireLock(perpetualLockId, category, ownerId, expiration))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  public void testTryAcquirePerpetualLock_existsAndReserveSuccess() {
    // given: DAO setup with existing lock and reserve success
    final String lockId = "test-lock-successful";
    final long reserveTime = 30;

    // then: lock acquired
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime)).isTrue();

    // and: expiration date in the future as expected
    PerpetualLock lock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(lock.getExpirationTime()).isAfter(new Date(currentTimeMillis() - 1_000 * (reserveTime - 2)));
    assertThat(lock.getExpirationTime()).isBefore(new Date(currentTimeMillis() + 1_000 * (reserveTime + 2)));
  }

  @Test
  public void testTryAcquirePerpetualLock_reserveTimeExtended() {
    // given: DAO setup with existing lock and reserve success
    final String lockId = "test-lock-reserve-time-extended";
    final long reserveTime = 30;

    // then: lock acquired
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime)).isTrue();
    // extend the lock with another call
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime + 10)).isTrue();

    // and: expiration date in the future as expected, with the newer expiration
    PerpetualLock lock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(lock.getExpirationTime()).isAfter(new Date(currentTimeMillis() - 1_000 * (reserveTime - 9)));
    assertThat(lock.getExpirationTime()).isBefore(new Date(currentTimeMillis() + 1_000 * (reserveTime + 11)));
  }

  @Test
  public void testTryAcquirePerpetualLock_reserveTimeNotReduced() {
    // given: DAO setup with existing lock and reserve success
    final String lockId = "test-lock-reserve-time-not-reduced";
    final long reserveTime = 30;

    // then: lock acquired
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime)).isTrue();
    // acquire the lock again, but with a shorter reserve time
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime - 10)).isTrue();

    // and: expiration date in the future as expected, with the original expiration
    PerpetualLock lock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(lock.getExpirationTime()).isAfter(new Date(currentTimeMillis() - 1_000 * (reserveTime - 2)));
    assertThat(lock.getExpirationTime()).isBefore(new Date(currentTimeMillis() + 1_000 * (reserveTime + 2)));
  }

  @Test
  public void testTryAcquirePerpetualLock_existsAndReserveUnsuccessful() {
    // given: DAO setup with existing lock and reserve unsuccessful
    final String lockId = "test-lock-unsuccessful";
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", 30)).isTrue();

    // then:
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner1", 30)).isFalse();
  }

  @Test
  public void testTryAcquirePerpetualLock_doesntExistYetAndCreateSuccessful() {
    // given: DAO setup with no existing lock and create successful
    final String lockId = "abc123";

    // then:
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "xyz45", 30)).isTrue();
    assertThat(logOutput).atTraceLevel()
        .contains(
            "Trying to acquire perpetual lock abc12 on behalf of xyz45 to expire in 30 seconds.");
    assertThat(logOutput).atTraceLevel()
        .contains("Perpetual lock abc12 does not exist yet.  Creating...");
    assertThat(logOutput).atTraceLevel()
        .contains("Perpetual lock abc12 created and acquired on behalf of xyz45.");
  }

  @Test
  public void testTryAcquirePerpetualLock_doesntExistYetAndCreateUnsuccessfulAndReserveSuccessful() {
    // given: DAO setup with no existing lock and create setup to throw exception with subsequent reservation succeeding
    final String lockId = "abc123";
    final String owner = "xyz456";
    final String category = "testing";
    final long expiration = 30;
    PerpetualLockDAO spyPerpetualLockDAO = spy(perpetualLockDAO);
    perpetualLockManager = new PerpetualLockManager(spyPerpetualLockDAO);
    // Simulate a unique constraint violation (SQL state 23505 = unique_violation)
    org.postgresql.util.PSQLException psqlEx = new org.postgresql.util.PSQLException(
        "duplicate key value violates unique constraint", org.postgresql.util.PSQLState.UNIQUE_VIOLATION);
    doThrow(new DataAccessException("insert failed", psqlEx)).when(spyPerpetualLockDAO)
        .createPerpetualLock(eq(lockId), eq(category), eq(owner), any(Date.class));
    doReturn(1).when(spyPerpetualLockDAO).reservePerpetualLock(eq(lockId), eq(owner), any(Date.class));

    // then:
    assertThat(perpetualLockManager.tryAcquireLock(lockId, category, owner, expiration)).isTrue();
    assertThat(logOutput).atTraceLevel()
        .contains("Perpetual lock abc12 already exists.  " +
            "Will try to reserve it now on behalf of xyz45.");
  }

  @Test
  public void testTryAcquirePerpetualLock_doesntExistYetAndCreateUnsuccessfulAndReserveUnsuccessful() {
    // given: DAO setup with no existing lock and create setup to throw exception with subsequent reservation failing
    final String lockId = "abc123";
    final String category = "testing";
    final String owner = "xyz456";
    final long expiration = 30;
    PerpetualLockDAO spyPerpetualLockDAO = spy(perpetualLockDAO);
    perpetualLockManager = new PerpetualLockManager(spyPerpetualLockDAO);
    // Simulate a unique constraint violation (SQL state 23505 = unique_violation)
    org.postgresql.util.PSQLException psqlEx = new org.postgresql.util.PSQLException(
        "duplicate key value violates unique constraint", org.postgresql.util.PSQLState.UNIQUE_VIOLATION);
    doThrow(new DataAccessException("insert failed", psqlEx)).when(spyPerpetualLockDAO)
        .createPerpetualLock(eq(lockId), eq(category), eq(owner), any(Date.class));

    // then:
    assertThat(perpetualLockManager.tryAcquireLock(lockId, category, owner, expiration)).isFalse();
    assertThat(logOutput).atTraceLevel()
        .contains("Perpetual lock abc12 already exists.  "
            + "Will try to reserve it now on behalf of xyz45.");
  }

  @Test
  public void testReleasePerpetualLock() {
    String lockId = "test-lock-release";
    String owner = "test-owner";
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", owner, 30)).isTrue();

    // when:
    perpetualLockManager.releasePerpetualLock(lockId, owner);

    // then:
    PerpetualLock lock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(lock.getOwner()).isNull();
    assertThat(lock.getExpirationTime()).isNull();
  }
}
