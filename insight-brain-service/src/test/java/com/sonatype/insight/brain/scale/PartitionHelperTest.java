/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scale;

import java.util.ArrayList;
import java.util.Date;

import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.model.PerpetualLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class PartitionHelperTest
{
  @Mock
  private PerpetualLockManager mockPerpetualLockManager;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testComputeIdealPartitionCount() {
    // given:
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA");

    // when: analyze various combinations partitions and instances and verify results
    assertThat(partitionHelper.computeIdealPartitionCount(0, 0)).isEqualTo(1);
    assertThat(partitionHelper.computeIdealPartitionCount(0, 1)).isEqualTo(1);
    assertThat(partitionHelper.computeIdealPartitionCount(1, 0)).isEqualTo(1);
    assertThat(partitionHelper.computeIdealPartitionCount(5, 2)).isEqualTo(3);
    assertThat(partitionHelper.computeIdealPartitionCount(2, 5)).isEqualTo(1);
    assertThat(partitionHelper.computeIdealPartitionCount(1, 10)).isEqualTo(1);
    assertThat(partitionHelper.computeIdealPartitionCount(10, 10)).isEqualTo(1);
    assertThat(partitionHelper.computeIdealPartitionCount(10, 2)).isEqualTo(5);
  }

  @Test
  public void testCanTryToUsePartition_alreadyInUseByAnotherInstance() {
    // given: a lock in place by another instance for the target partition
    final String partitionInUse = "partitionInUse";
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory("testCategory")).thenReturn(
        initLocks("testCategory")
            .withHeartbeatLock("instanceA", false)
            .withHeartbeatLock("instanceB", false)
            .withLock(partitionInUse, "instanceB", false));
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA")
        .withPerpetualLockManager(mockPerpetualLockManager);

    // when:
    boolean canTryToUse = partitionHelper.canTryToUsePartition(partitionInUse);

    // then:
    assertThat(canTryToUse).isFalse();
  }

  @Test
  public void testCanTryToUsePartition_alreadyInUseByThisInstance() {
    // given: a lock already held by the same instance for the partition
    final String partitionInUse = "partitionInUse";
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory("testCategory")).thenReturn(
        initLocks("testCategory")
            .withHeartbeatLock("instanceA", false)
            .withHeartbeatLock("instanceB", false)
            .withLock(partitionInUse, "instanceA", false));
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA")
        .withPerpetualLockManager(mockPerpetualLockManager);

    // when:
    boolean canTryToUse = partitionHelper.canTryToUsePartition(partitionInUse);

    // then:
    assertThat(canTryToUse).isTrue();
  }

  @Test
  public void testCanTryToUsePartition_notCurrentlyInUseByAnyInstance() {
    // given: no locks currently being held (other than heartbeat locks)
    final String partitionAvailable = "partitionAvailable";
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory("testCategory")).thenReturn(
        initLocks("testCategory")
            .withHeartbeatLock("instanceA", false)
            .withHeartbeatLock("instanceB", false));
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA")
        .withPerpetualLockManager(mockPerpetualLockManager);

    // when:
    boolean canTryToUse = partitionHelper.canTryToUsePartition(partitionAvailable);

    // then:
    assertThat(canTryToUse).isTrue();
  }

  @Test
  public void testCanTryToUsePartition_atOrAbovePartitionLimit() {
    // given: simulate a new instance coming online when target instance already has several partition locks
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory("testCategory")).thenReturn(
        initLocks("testCategory")
            .withHeartbeatLock("instanceA", false)
            .withHeartbeatLock("instanceB", false)
            .withLock("partition1", "instanceA", false)
            .withLock("partition2", "instanceA", false)
            .withLock("partition3", "instanceA", false));
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA")
        .withPerpetualLockManager(mockPerpetualLockManager);

    // when:
    boolean canTryToUse = partitionHelper.canTryToUsePartition("partition4");

    // then: expect it can't use since it already has too many partitions
    assertThat(canTryToUse).isFalse();

    // when: the other instance tries to use the partition
    PartitionHelper instance2PartitionHelper = new PartitionHelper("testCategory", "instanceB")
        .withPerpetualLockManager(mockPerpetualLockManager);
    canTryToUse = instance2PartitionHelper.canTryToUsePartition("partition4");

    // then: it should be able to
    assertThat(canTryToUse).isTrue();
  }

  @Test
  public void testCanTryToUsePartition_reserveOutcomeFail() {
    // given: no locks currently being held (other than heartbeat locks)
    final String partitionAvailable = "partitionAvailable";
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory("testCategory")).thenReturn(
        initLocks("testCategory")
            .withHeartbeatLock("instanceA", false)
            .withHeartbeatLock("instanceB", false));
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA")
        .withPerpetualLockManager(mockPerpetualLockManager);

    // when:
    boolean canTryToUse = partitionHelper.canTryToUsePartition(partitionAvailable);

    // then:
    assertThat(canTryToUse).isTrue();

    // when: mark the subsequent partition reservation as unsuccessful and try again to see if we can use the partition
    partitionHelper.setPartitionReservationOutcome(partitionAvailable, false);
    canTryToUse = partitionHelper.canTryToUsePartition(partitionAvailable);

    // then: we can't use it now since we were unable to reserve it
    assertThat(canTryToUse).isFalse();
  }

  @Test
  public void testCanTryToUsePartition_reserveOutcomeSuccess() {
    // given: no locks currently being held (other than heartbeat locks)
    final String partitionAvailable = "partitionAvailable";
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory("testCategory")).thenReturn(
        initLocks("testCategory")
            .withHeartbeatLock("instanceA", false)
            .withHeartbeatLock("instanceB", false));
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA")
        .withPerpetualLockManager(mockPerpetualLockManager);

    // when:
    boolean canTryToUse = partitionHelper.canTryToUsePartition(partitionAvailable);

    // then:
    assertThat(canTryToUse).isTrue();

    // when: mark the subsequent partition reservation as successful and try again to see if we can use the partition
    partitionHelper.setPartitionReservationOutcome(partitionAvailable, true);
    canTryToUse = partitionHelper.canTryToUsePartition(partitionAvailable);

    // then: we are still able to use it
    assertThat(canTryToUse).isTrue();
  }

  @Test
  public void testCanTryToUsePartition_otherInstanceGoesOffline() {
    // given: simulate a new instance coming online when target instance already has several partition locks
    PerpetualLockTestList perpetualLocks = initLocks("testCategory")
        .withHeartbeatLock("instanceA", false)
        .withHeartbeatLock("instanceB", false)
        .withLock("partition1", "instanceA", false)
        .withLock("partition2", "instanceA", false)
        .withLock("partition3", "instanceA", false)
        .withLock("partition4", "instanceB", false)
        .withLock("partition5", "instanceB", false)
        .withLock("partition6", "instanceB", false);

    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory("testCategory")).thenReturn(perpetualLocks);
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA")
        .withPerpetualLockManager(mockPerpetualLockManager);

    // when:
    boolean canTryToUse = partitionHelper.canTryToUsePartition("partition4");

    // then: expect it can't use since it's being used by instanceB'
    assertThat(canTryToUse).isFalse();

    // when: expire the locks for instanceB, reset partition analysis to force a new one, and try again
    perpetualLocks.takeInstanceOffline("instanceB");
    partitionHelper.resetPartitionAnalysis();
    canTryToUse = partitionHelper.canTryToUsePartition("partition4");

    // then:
    assertThat(canTryToUse).isTrue();
  }

  @Test
  public void testReservationCache_validWithinTtl() {
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA");

    partitionHelper.cacheReservation("partition1", 60);

    assertThat(partitionHelper.isReservationValid("partition1")).isTrue();
  }

  @Test
  public void testReservationCache_invalidForUnknownPartition() {
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA");

    assertThat(partitionHelper.isReservationValid("unknown")).isFalse();
  }

  @Test
  public void testReservationCache_notCachedWhenDurationIsZero() {
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA");

    partitionHelper.cacheReservation("partition1", 0);

    assertThat(partitionHelper.isReservationValid("partition1")).isFalse();
  }

  @Test
  public void testReservationCache_clearedOnPartitionAnalysis() {
    when(mockPerpetualLockManager.getAllActivePerpetualLocksForCategory("testCategory")).thenReturn(
        initLocks("testCategory")
            .withHeartbeatLock("instanceA", false));
    PartitionHelper partitionHelper = new PartitionHelper("testCategory", "instanceA")
        .withPerpetualLockManager(mockPerpetualLockManager);

    partitionHelper.cacheReservation("partition1", 60);

    assertThat(partitionHelper.isReservationValid("partition1")).isTrue();

    // trigger partition analysis by resetting timeout and calling canTryToUsePartition
    partitionHelper.resetPartitionAnalysis();
    partitionHelper.canTryToUsePartition("anyPartition");

    assertThat(partitionHelper.isReservationValid("partition1")).isFalse();
  }

  private PerpetualLockTestList initLocks(String category) {
    return new PerpetualLockTestList(category);
  }

  private class PerpetualLockTestList
      extends ArrayList<PerpetualLock>
  {
    private final String category;

    PerpetualLockTestList(String category) {
      this.category = category;
    }

    PerpetualLockTestList withHeartbeatLock(String id, boolean isExpired) {
      return withLock(id, id, isExpired);
    }

    PerpetualLockTestList withLock(String id, String owner, boolean isExpired) {
      long expiration = System.currentTimeMillis() + (isExpired ? -1_000 : 10_000);
      add(new PerpetualLock(category, id).setOwner(owner).setExpirationTime(new Date(expiration)));
      return this;
    }

    PerpetualLockTestList takeInstanceOffline(String instanceId) {
      // remove all the locks for the given instance
      for (int i = 0; i < size();) {
        if (instanceId.equals(get(i).getOwner())) {
          remove(i);
        }
        else {
          i++;
        }
      }
      return this;
    }
  } // class PerpetualLockTestList
}
