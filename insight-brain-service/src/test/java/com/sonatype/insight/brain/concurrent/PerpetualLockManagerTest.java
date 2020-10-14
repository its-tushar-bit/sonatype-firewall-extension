/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.concurrent;

import java.util.Date;

import javax.persistence.EntityExistsException;
import javax.persistence.RollbackException;

import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.git.VerifiableLoggingTestBase;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PerpetualLockManagerTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private PerpetualLockDAO mockPerpetualLockDAO;

  @Mock
  private TransactionContext mockTxn;

  // subject
  private PerpetualLockManager perpetualLockManager;

  public PerpetualLockManagerTest() {
    super(PerpetualLockManager.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
    perpetualLockManager = new PerpetualLockManager(mockPerpetualLockDAO);
    doReturn(mockTxn).when(mockPerpetualLockDAO).createTransactionContext();
  }

  @Test
  public void testTryAcquirePerpetualLock_invalidArgs() {
    // when: id is blank
    assertIllegalArgumentExceptionThrownOnTryAcquireLock(" ", "owner", 1,
        "Required perpetualLockId is blank or missing.");

    // when: id is null
    assertIllegalArgumentExceptionThrownOnTryAcquireLock(null, "owner", 1,
        "Required perpetualLockId is blank or missing.");

    // when: owner is blank
    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", " ", 1,
        "Required perpetual lock owner is blank or missing.");

    // when: owner is null
    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", null, 1,
        "Required perpetual lock owner is blank or missing.");

    // when: expires is 0
    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", "owner", 0,
        "Perpetual lock expiration of 0 seconds is invalid.");

    // when: expires is negative
    assertIllegalArgumentExceptionThrownOnTryAcquireLock("id", "owner", -1,
        "Perpetual lock expiration of -1 seconds is invalid.");
  }

  private void assertIllegalArgumentExceptionThrownOnTryAcquireLock(
      String perpetualLockId,
      String ownerId,
      long expiration,
      String expectedMessage)
  {
    assertThatThrownBy(() -> {
      perpetualLockManager.tryAcquireLock(perpetualLockId, ownerId, expiration);
    }).isInstanceOf(IllegalArgumentException.class).hasMessage(expectedMessage);
  }

  @Test
  public void testTryAcquirePerpetualLock_existsAndReserveSuccess() {
    // given: DAO setup with existing lock and reserve success
    final String lockId = "test-lock-successful";
    final long reserveTime = 30;
    doReturn(new PerpetualLock()).when(mockPerpetualLockDAO).getPerpetualLockByIdForUpdate(eq(mockTxn), eq(lockId));
    doReturn(1).when(mockPerpetualLockDAO)
        .reservePerpetualLock(eq(mockTxn), eq(lockId), eq("test-owner"), any(Date.class));

    // then: lock acquired
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "test-owner", reserveTime)).isTrue();

    // and: expiration date in the future as expected
    ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
    verify(mockPerpetualLockDAO).reservePerpetualLock(eq(mockTxn), eq(lockId), eq("test-owner"), dateCaptor.capture());
    assertThat(dateCaptor.getValue()).isAfter(new Date(currentTimeMillis() - 1_000 * (reserveTime - 2)));
    assertThat(dateCaptor.getValue()).isBefore(new Date(currentTimeMillis() + 1_000 * (reserveTime + 2)));
  }

  @Test
  public void testTryAcquirePerpetualLock_existsAndReserveUnsuccessful() {
    // given: DAO setup with existing lock and reserve unsuccessful
    final String lockId = "test-lock-unsuccessful";
    doReturn(new PerpetualLock()).when(mockPerpetualLockDAO).getPerpetualLockByIdForUpdate(eq(mockTxn), eq(lockId));
    doReturn(0).when(mockPerpetualLockDAO)
        .reservePerpetualLock(eq(mockTxn), eq(lockId), eq("test-owner"), any(Date.class));

    // then:
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "test-owner", 30)).isFalse();
  }

  @Test
  public void testTryAcquirePerpetualLock_doesntExistYetAndCreateSuccessful() {
    // given: DAO setup with no existing lock and create successful
    final String lockId = "test-lock-not-exists";
    doReturn(null).when(mockPerpetualLockDAO).getPerpetualLockByIdForUpdate(eq(mockTxn), eq(lockId));

    // then:
    assertThat(perpetualLockManager.tryAcquireLock(lockId, "test-owner", 30)).isTrue();
    assertThatLogMessagesEqual(
        trace("Trying to acquire perpetual lock test-lock-not-exists on behalf of test-owner to expire in 30 seconds."),
        trace("Perpetual lock test-lock-not-exists does not exist yet.  Creating..."),
        trace("Perpetual lock test-lock-not-exists created and acquired on behalf of test-owner.")
    );
  }

  @Test
  public void testTryAcquirePerpetualLock_doesntExistYetAndCreateUnsuccessfulAndReserveSuccessful() {
    // given: DAO setup with no existing lock and create setup to throw exception with subsequent reservation succeeding
    final String lockId = "test-lock-simultaneous";
    final String owner = "test-owner";
    final long expiration = 30;
    doReturn(null).when(mockPerpetualLockDAO).getPerpetualLockByIdForUpdate(eq(mockTxn), eq(lockId));
    doThrow(new RollbackException(new EntityExistsException())).when(mockPerpetualLockDAO)
        .createPerpetualLock(eq(lockId), eq(owner), any(Date.class));
    doReturn(1).when(mockPerpetualLockDAO)
        .reservePerpetualLock(eq(lockId), eq(owner), any(Date.class));

    // then:
    assertThat(perpetualLockManager.tryAcquireLock(lockId, owner, expiration)).isTrue();
    assertThatLogMessagesContain(
        trace("Perpetual lock test-lock-simultaneous already exists.  " +
            "Will try to reserve it now on behalf of test-owner.")
    );
  }

  @Test
  public void testTryAcquirePerpetualLock_doesntExistYetAndCreateUnsuccessfulAndReserveUnsuccessful() {
    // given: DAO setup with no existing lock and create setup to throw exception with subsequent reservation failing
    final String lockId = "test-lock-simultaneous-reserve-fail";
    final String owner = "test-owner";
    final long expiration = 30;
    doReturn(null).when(mockPerpetualLockDAO).getPerpetualLockByIdForUpdate(eq(mockTxn), eq(lockId));
    doThrow(new RollbackException(new EntityExistsException())).when(mockPerpetualLockDAO)
        .createPerpetualLock(eq(lockId), eq(owner), any(Date.class));
    doReturn(0).when(mockPerpetualLockDAO)
        .reservePerpetualLock(eq(lockId), eq(owner), any(Date.class));

    // then:
    assertThat(perpetualLockManager.tryAcquireLock(lockId, owner, expiration)).isFalse();
    assertThatLogMessagesContain(
        trace("Perpetual lock test-lock-simultaneous-reserve-fail already exists.  " +
                "Will try to reserve it now on behalf of test-owner.")
    );
  }

  @Test
  public void testReleasePerpetualLock() {
    // when:
    perpetualLockManager.releasePerpetualLock("test-lock", "test-owner");

    // then:
    verify(mockPerpetualLockDAO, times(1)).releasePerpetualLockForOwner(eq("test-lock"), eq("test-owner"));
  }
}
