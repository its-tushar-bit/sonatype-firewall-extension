/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scale;

import com.sonatype.insight.brain.concurrent.PerpetualLockManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeartbeatPartitionManagerTest
{
  private static final String INSTANCE_ID = "instance-1";

  private static final String CATEGORY = "testing";

  private static final long PARTITION_RESERVATION_SECONDS = 30L;

  @Mock
  private PerpetualLockManager mockPerpetualLockManager;

  private HeartbeatPartitionManager heartbeatPartitionManager;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    heartbeatPartitionManager = new HeartbeatPartitionManager(mockPerpetualLockManager);
  }

  @Test
  public void heartbeatTaskBodyDelegatesToTryAcquireLockOnEachInvocation() {
    // given: the lock manager accepts renewals
    when(mockPerpetualLockManager.tryAcquireLock(anyString(), anyString(), anyString(), anyLong()))
        .thenReturn(true);

    Runnable heartbeatTaskBody = heartbeatPartitionManager.createHeartbeatTaskBody(
        INSTANCE_ID, CATEGORY, PARTITION_RESERVATION_SECONDS);

    // when: the scheduler would run the task body multiple times
    heartbeatTaskBody.run();
    heartbeatTaskBody.run();

    // then: each invocation attempted a lock renewal for this instance
    verify(mockPerpetualLockManager, times(2))
        .tryAcquireLock(INSTANCE_ID, CATEGORY, INSTANCE_ID, PARTITION_RESERVATION_SECONDS);
  }

  @Test
  public void heartbeatTaskBodySwallowsExceptionsFromPerpetualLockManager() {
    // given: a transient backend failure (e.g. Aurora failover) that causes every acquisition attempt
    // to throw a RuntimeException. If scheduleAtFixedRate were allowed to see this exception propagate,
    // it would silently cancel every future heartbeat execution, permanently disabling the scheduler
    // on this JVM. The task body must therefore swallow the exception internally.
    when(mockPerpetualLockManager.tryAcquireLock(anyString(), anyString(), anyString(), anyLong()))
        .thenThrow(new RuntimeException("simulated DB failure"));

    Runnable heartbeatTaskBody = heartbeatPartitionManager.createHeartbeatTaskBody(
        INSTANCE_ID, CATEGORY, PARTITION_RESERVATION_SECONDS);

    // when: the scheduler tries to run the task three times in a row while the backend is unhealthy
    heartbeatTaskBody.run();
    heartbeatTaskBody.run();
    heartbeatTaskBody.run();

    // then: none of the invocations propagated the exception (implicit: no test failure above) and every
    // invocation actually reached the lock-acquisition call. If the task body had not been defensive, the
    // first RuntimeException would have escaped the test invocation chain and the assertion below would
    // never run; in the real system, scheduleAtFixedRate would see the uncaught throw and cancel future
    // executions of this task.
    verify(mockPerpetualLockManager, times(3))
        .tryAcquireLock(INSTANCE_ID, CATEGORY, INSTANCE_ID, PARTITION_RESERVATION_SECONDS);
  }

  @Test
  public void heartbeatTaskBodyPropagatesErrorsRatherThanSwallowingThem() {
    // given: a JVM-level failure such as OutOfMemoryError or LinkageError. Unlike transient
    // RuntimeExceptions from the backend, Errors signal that the JVM is in trouble. Continuing to
    // call the database on a 60-second cadence as if nothing were wrong is actively harmful --
    // for example, an OutOfMemoryError means allocating another log line (which this wrapper
    // would do if it swallowed the Error) can itself fail and mask the real problem. The correct
    // failure mode is: let the scheduler cancel the task, let this instance's heartbeat lock
    // expire naturally, and let monitoring/other instances notice the sick JVM.
    when(mockPerpetualLockManager.tryAcquireLock(anyString(), anyString(), anyString(), anyLong()))
        .thenThrow(new OutOfMemoryError("simulated JVM error"));

    Runnable heartbeatTaskBody = heartbeatPartitionManager.createHeartbeatTaskBody(
        INSTANCE_ID, CATEGORY, PARTITION_RESERVATION_SECONDS);

    // then: the Error propagates out of the task body. In production,
    // ScheduledExecutorService.scheduleAtFixedRate will see the uncaught Error and cancel future
    // executions of this task -- which is the desired behavior when the JVM is unhealthy.
    assertThatThrownBy(heartbeatTaskBody::run)
        .isInstanceOf(OutOfMemoryError.class)
        .hasMessage("simulated JVM error");
  }
}
