/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.concurrent;

import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;

import java.util.Date;
import jakarta.inject.Inject;

import ch.qos.logback.classic.Level;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@ComponentH2Test
public class PerpetualLockManagerTest
    extends AbstractComponentH2Test
{
  public LogOutput logOutput = new LogOutput(PerpetualLockManager.class);

  @Inject
  private PerpetualLockDAO perpetualLockDAO;

  // subject
  private PerpetualLockManager perpetualLockManager;

  @BeforeEach
  public void before() {
    logOutput.setLogLevel(Level.TRACE);

    perpetualLockManager = new PerpetualLockManager(perpetualLockDAO);
    assertThat(perpetualLockDAO.getAll()).hasSize(0);
  }

  @Test
  public void testTryAcquirePerpetualLock_invalidArgs() {
    assertIllegalArgumentExceptionThrownOnTryAcquireLock(" ", "cat", "owner", 1,
        "Required perpetualLockId is blank or missing.");

    assertIllegalArgumentExceptionThrownOnTryAcquireLock(null, "cat", "owner", 1,
        "Required perpetualLockId is blank or missing.");

    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", "cat", " ", 1,
        "Required perpetual lock owner is blank or missing.");

    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", "cat", null, 1,
        "Required perpetual lock owner is blank or missing.");

    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", "cat", "owner", 0,
        "Perpetual lock expiration of 0 seconds is invalid.");

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
    final String lockId = "test-lock-successful";
    final long reserveTime = 30;

    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime)).isTrue();

    PerpetualLock lock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(lock.getExpirationTime()).isAfter(new Date(currentTimeMillis() - 1_000 * (reserveTime - 2)));
    assertThat(lock.getExpirationTime()).isBefore(new Date(currentTimeMillis() + 1_000 * (reserveTime + 2)));
  }

  @Test
  public void testTryAcquirePerpetualLock_reserveTimeExtended() {
    final String lockId = "test-lock-reserve-time-extended";
    final long reserveTime = 30;

    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime)).isTrue();
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime + 10)).isTrue();

    PerpetualLock lock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(lock.getExpirationTime()).isAfter(new Date(currentTimeMillis() - 1_000 * (reserveTime - 9)));
    assertThat(lock.getExpirationTime()).isBefore(new Date(currentTimeMillis() + 1_000 * (reserveTime + 11)));
  }

  @Test
  public void testTryAcquirePerpetualLock_reserveTimeNotReduced() {
    final String lockId = "test-lock-reserve-time-not-reduced";
    final long reserveTime = 30;

    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime)).isTrue();
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", reserveTime - 10)).isTrue();

    PerpetualLock lock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(lock.getExpirationTime()).isAfter(new Date(currentTimeMillis() - 1_000 * (reserveTime - 2)));
    assertThat(lock.getExpirationTime()).isBefore(new Date(currentTimeMillis() + 1_000 * (reserveTime + 2)));
  }

  @Test
  public void testTryAcquirePerpetualLock_existsAndReserveUnsuccessful() {
    final String lockId = "test-lock-unsuccessful";
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner", 30)).isTrue();

    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "test-owner1", 30)).isFalse();
  }

  @Test
  public void testTryAcquirePerpetualLock_doesntExistYetAndCreateSuccessful() {
    final String lockId = "abc123";

    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "xyz45", 30)).isTrue();
    assertThat(logOutput).atTraceLevel()
        .contains("Trying to acquire perpetual lock abc12 on behalf of xyz45 to expire in 30 seconds.");
    assertThat(logOutput).atTraceLevel()
        .contains("Perpetual lock abc12 on behalf of xyz45 was acquired.");
  }

  @Test
  public void testTryAcquirePerpetualLock_concurrentCreateHandledByUpsert() {
    final String lockId = "abc123";

    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "owner1", 30)).isTrue();
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "owner2", 30)).isFalse();
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", "owner1", 30)).isTrue();
  }

  @Test
  public void testTryAcquirePerpetualLock_daoThrows_returnsFalse() {
    PerpetualLockDAO spyDAO = spy(perpetualLockDAO);
    perpetualLockManager = new PerpetualLockManager(spyDAO);
    doThrow(new DataAccessException("connection lost"))
        .when(spyDAO)
        .tryAcquireOrRenewLock(eq("abc123"), eq("testing"), eq("xyz456"), any(Date.class));

    assertThat(perpetualLockManager.tryAcquireLock("abc123", "testing", "xyz456", 30)).isFalse();
  }

  @Test
  public void testReleasePerpetualLock() {
    String lockId = "test-lock-release";
    String owner = "test-owner";
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "testing", owner, 30)).isTrue();

    perpetualLockManager.releasePerpetualLock(lockId, owner);

    PerpetualLock lock = perpetualLockDAO.getPerpetualLockById(lockId);
    assertThat(lock.getOwner()).isNull();
    assertThat(lock.getExpirationTime()).isNull();
  }
}
