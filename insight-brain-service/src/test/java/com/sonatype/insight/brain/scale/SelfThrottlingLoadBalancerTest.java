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
import com.sonatype.insight.brain.tenancy.TenantUtil;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SelfThrottlingLoadBalancerTest
{
  private static final String TEST_CATEGORY = "testing";

  @Mock
  private HeartbeatPartitionManager mockHeartbeatPartitionManager;

  @Mock
  private PerpetualLockManager mockPerpetualLockManager;

  @Mock
  private TenantUtil mockTenantUtil;

  private TestableSelfThrottlingLoadBalancer testableSelfThrottlingLoadBalancer;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    testableSelfThrottlingLoadBalancer =
        new TestableSelfThrottlingLoadBalancer(mockHeartbeatPartitionManager, mockPerpetualLockManager, TEST_CATEGORY,
            mockTenantUtil);
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
        new PerpetualLock(TEST_CATEGORY, "somePartition").setOwner("otherInstance").setExpirationTime(futureDate));
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
            .setExpirationTime(futureDate));
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
            .setExpirationTime(futureDate));
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

  @Test
  public void testStart_MultiTenant_BatchNode() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(true);
    when(mockTenantUtil.isMtiqBatchMode()).thenReturn(true);

    testableSelfThrottlingLoadBalancer.start();

    verify(mockHeartbeatPartitionManager).start(anyString(), anyString(), anyLong());
  }

  @Test
  public void testStop_MultiTenant_BatchNode() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(true);
    when(mockTenantUtil.isMtiqBatchMode()).thenReturn(true);

    testableSelfThrottlingLoadBalancer.stop();

    verify(mockHeartbeatPartitionManager).stop();
  }

  @Test
  public void testStart_MultiTenant_NotBatchNode() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(true);
    when(mockTenantUtil.isMtiqBatchMode()).thenReturn(false);

    testableSelfThrottlingLoadBalancer.start();

    verifyNoInteractions(mockHeartbeatPartitionManager);
  }

  @Test
  public void testStop_MultiTenant_NotBatchNode() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(true);
    when(mockTenantUtil.isMtiqBatchMode()).thenReturn(false);

    testableSelfThrottlingLoadBalancer.stop();

    verifyNoInteractions(mockHeartbeatPartitionManager);
  }

  @Test
  public void testStart_SingleTenant_BatchNode() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(false);
    when(mockTenantUtil.isMtiqBatchMode()).thenReturn(true);

    testableSelfThrottlingLoadBalancer.start();

    verify(mockHeartbeatPartitionManager).start(anyString(), anyString(), anyLong());
  }

  @Test
  public void testStop_SingleTenant_BatchNode() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(false);
    when(mockTenantUtil.isMtiqBatchMode()).thenReturn(true);

    testableSelfThrottlingLoadBalancer.stop();

    verify(mockHeartbeatPartitionManager).stop();
  }

  @Test
  public void testStart_SingleTenant_NotBatchNode() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(false);
    when(mockTenantUtil.isMtiqBatchMode()).thenReturn(false);

    testableSelfThrottlingLoadBalancer.start();

    verify(mockHeartbeatPartitionManager).start(anyString(), anyString(), anyLong());
  }

  @Test
  public void testStop_SingleTenant_NotBatchNode() {
    when(mockTenantUtil.isMultiTenant()).thenReturn(false);
    when(mockTenantUtil.isMtiqBatchMode()).thenReturn(false);

    testableSelfThrottlingLoadBalancer.stop();

    verify(mockHeartbeatPartitionManager).stop();
  }

  @Test
  public void testCanUsePartition_cachedReservationSkipsDbCall() {
    // given: single-instance mode (no other heartbeats) with a successful reservation
    String instanceId = testableSelfThrottlingLoadBalancer.getInstanceId();
    Date futureDate = new Date(System.currentTimeMillis() + 60_000);
    List<PerpetualLock> perpetualLocks = ImmutableList.of(
        new PerpetualLock(TEST_CATEGORY, instanceId).setOwner(instanceId).setExpirationTime(futureDate));
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory(TEST_CATEGORY)).thenReturn(perpetualLocks);
    when(mockPerpetualLockManager.tryAcquireLock(
        "somePartition", TEST_CATEGORY, instanceId,
        TestableSelfThrottlingLoadBalancer.DEFAULT_PARTITION_RESERVATION_SECONDS)).thenReturn(true);

    // when: first call acquires the lock
    boolean firstCall = testableSelfThrottlingLoadBalancer.canUsePartition("somePartition");

    // then: lock was acquired via DB
    assertThat(firstCall).isTrue();
    verify(mockPerpetualLockManager, times(1)).tryAcquireLock(
        "somePartition", TEST_CATEGORY, instanceId,
        TestableSelfThrottlingLoadBalancer.DEFAULT_PARTITION_RESERVATION_SECONDS);

    // when: second call should use the cache
    boolean secondCall = testableSelfThrottlingLoadBalancer.canUsePartition("somePartition");

    // then: still returns true but did NOT call tryAcquireLock again
    assertThat(secondCall).isTrue();
    verify(mockPerpetualLockManager, times(1)).tryAcquireLock(
        "somePartition", TEST_CATEGORY, instanceId,
        TestableSelfThrottlingLoadBalancer.DEFAULT_PARTITION_RESERVATION_SECONDS);
  }

  @Test
  public void testCanUsePartition_failedReservationNotCached() {
    // given: single-instance mode with a failed reservation
    String instanceId = testableSelfThrottlingLoadBalancer.getInstanceId();
    Date futureDate = new Date(System.currentTimeMillis() + 60_000);
    List<PerpetualLock> perpetualLocks = ImmutableList.of(
        new PerpetualLock(TEST_CATEGORY, instanceId).setOwner(instanceId).setExpirationTime(futureDate));
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory(TEST_CATEGORY)).thenReturn(perpetualLocks);
    when(mockPerpetualLockManager.tryAcquireLock(
        "somePartition", TEST_CATEGORY, instanceId,
        TestableSelfThrottlingLoadBalancer.DEFAULT_PARTITION_RESERVATION_SECONDS)).thenReturn(false);

    // when: first call fails to acquire
    testableSelfThrottlingLoadBalancer.canUsePartition("somePartition");

    // when: second call should retry (not cached)
    testableSelfThrottlingLoadBalancer.canUsePartition("somePartition");

    // then: tryAcquireLock was called twice (not cached on failure)
    verify(mockPerpetualLockManager, times(2)).tryAcquireLock(
        "somePartition", TEST_CATEGORY, instanceId,
        TestableSelfThrottlingLoadBalancer.DEFAULT_PARTITION_RESERVATION_SECONDS);
  }

  private class TestableSelfThrottlingLoadBalancer
      extends SelfThrottlingLoadBalancer
  {
    public TestableSelfThrottlingLoadBalancer(
        final HeartbeatPartitionManager heartbeatPartitionManager,
        final PerpetualLockManager perpetualLockManager,
        final String category,
        final TenantUtil tenantUtil)
    {
      super(heartbeatPartitionManager, perpetualLockManager, category, tenantUtil);
    }
  }
}
