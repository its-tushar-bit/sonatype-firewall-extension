/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scale;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.model.PerpetualLock;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SelfThrottlingLoadBalancerTest
{
  private static final String TEST_CATEGORY = "testing";

  @Mock
  private HeartbeatPartitionManager mockHeartbeatPartitionManager;

  @Mock
  private PerpetualLockManager mockPerpetualLockManager;

  private TestableSelfThrottlingLoadBalancer testableSelfThrottlingLoadBalancer;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    testableSelfThrottlingLoadBalancer =
        new TestableSelfThrottlingLoadBalancer(mockHeartbeatPartitionManager, mockPerpetualLockManager, TEST_CATEGORY);
  }

  @Test
  public void testStartStop() {
    // when:
    testableSelfThrottlingLoadBalancer.start();

    // then:
    verify(mockHeartbeatPartitionManager, times(1)).start(testableSelfThrottlingLoadBalancer.getInstanceId(),
        TEST_CATEGORY, SelfThrottlingLoadBalancer.DEFAULT_PARTITION_RESERVATION_SECONDS);
    verify(mockPerpetualLockManager, times(1)).removeExpiredLocks();

    // when:
    testableSelfThrottlingLoadBalancer.stop();

    // then:
    verify(mockHeartbeatPartitionManager, times(1)).stop();
    verify(mockPerpetualLockManager, times(1))
        .releasePerpetualLocksForOwner(testableSelfThrottlingLoadBalancer.getInstanceId());
  }

  @Test
  public void testCanUsePartition_alreadyReservedByOtherInstance() {
    // given: a partition assigned to some other instance
    Date futureDate = new Date(System.currentTimeMillis() + 60_000);
    List<PerpetualLock> perpetualLocks = ImmutableList.of(
        new PerpetualLock(TEST_CATEGORY, "somePartition").setOwner("otherInstance").setExpirationTime(futureDate)
    );
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory(TEST_CATEGORY)).thenReturn(perpetualLocks);

    // when: try to use that partition
    boolean canUse = testableSelfThrottlingLoadBalancer.canUsePartition("somePartition");

    // then:
    assertThat(canUse).isFalse();
  }

  @Test
  public void testCanUsePartition_reservePartitionFailed() {
    // given: a partition this instance can't get a lock for
    Date futureDate = new Date(System.currentTimeMillis() + 60_000);
    List<PerpetualLock> perpetualLocks = ImmutableList.of(
        new PerpetualLock(TEST_CATEGORY, "somePartition")
            .setOwner(testableSelfThrottlingLoadBalancer.getInstanceId())
            .setExpirationTime(futureDate)
    );
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory(TEST_CATEGORY)).thenReturn(perpetualLocks);
    when(mockPerpetualLockManager.tryAcquireLock(
        "somePartition",
        TEST_CATEGORY,
        testableSelfThrottlingLoadBalancer.getInstanceId(),
        TestableSelfThrottlingLoadBalancer.DEFAULT_PARTITION_RESERVATION_SECONDS)).thenReturn(false);

    // when: try to use the partition
    boolean canUse = testableSelfThrottlingLoadBalancer.canUsePartition("somePartition");

    // then:
    assertThat(canUse).isFalse();
  }

  @Test
  public void testCanUsePartition_reservePartitionSucceeded() {
    // given: a partition we can get the lock for
    Date futureDate = new Date(System.currentTimeMillis() + 60_000);
    List<PerpetualLock> perpetualLocks = ImmutableList.of(
        new PerpetualLock(TEST_CATEGORY, "somePartition")
            .setOwner(testableSelfThrottlingLoadBalancer.getInstanceId())
            .setExpirationTime(futureDate)
    );
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory(TEST_CATEGORY)).thenReturn(perpetualLocks);
    when(mockPerpetualLockManager.tryAcquireLock(
        "somePartition",
        TEST_CATEGORY,
        testableSelfThrottlingLoadBalancer.getInstanceId(),
        TestableSelfThrottlingLoadBalancer.DEFAULT_PARTITION_RESERVATION_SECONDS)).thenReturn(true);

    // when: try to use that partition
    boolean canUse = testableSelfThrottlingLoadBalancer.canUsePartition("somePartition");

    // then:
    assertThat(canUse).isTrue();
  }

  private class TestableSelfThrottlingLoadBalancer
      extends SelfThrottlingLoadBalancer
  {
    public TestableSelfThrottlingLoadBalancer(
        final HeartbeatPartitionManager heartbeatPartitionManager,
        final PerpetualLockManager perpetualLockManager,
        final String category)
    {
      super(heartbeatPartitionManager, perpetualLockManager, category);
    }
  }
}
