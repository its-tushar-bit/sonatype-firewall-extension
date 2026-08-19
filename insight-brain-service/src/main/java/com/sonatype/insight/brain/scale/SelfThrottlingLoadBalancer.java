/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scale;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;

/**
 * The self-throttling load balancer tries to control the workload of the client using it by examining the number
 * of partitions of work it's reserving on behalf of its client in relation to any other load balancer instances
 * that are also reserving partitions on behalf of their IQ server instances.
 *
 * A partition of work is whatever the client of the load balancer defines it to be.
 *
 * If the load balancer detects that it has reserved more work than what is ideal it will suspend one or more
 * partitions, deny new requests for those partitions, wait for the work already in progress to complete, and then
 * make the partition(s) available to other load balancer instances.
 *
 * Likewise, if it detects that it should approve more of the workload it will try to reserve new partitions
 * of work as they are requested.
 *
 * The load balancer does not do the work and doesn't direct other classes to do the work. It simply serves
 * as a coordinator
 */
public abstract class SelfThrottlingLoadBalancer
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(SelfThrottlingLoadBalancer.class);

  @VisibleForTesting
  static final int DEFAULT_PARTITION_RESERVATION_SECONDS = 60;

  private final String myInstanceId = UUID.randomUUID().toString();

  // specifies the category of work for which this load balancer is coordinating with other load balancers running
  // on other IQ instances
  private final String category;

  private final HeartbeatPartitionManager heartbeatPartitionManager;

  protected final PerpetualLockManager perpetualLockManager;

  private final PartitionHelper partitionHelper;

  private final TenantUtil tenantUtil;

  private int partitionReservationSeconds = DEFAULT_PARTITION_RESERVATION_SECONDS;

  public boolean disableForTesting;

  public SelfThrottlingLoadBalancer(
      final HeartbeatPartitionManager heartbeatPartitionManager,
      final PerpetualLockManager perpetualLockManager,
      final String category,
      final TenantUtil tenantUtil)
  {
    validateInitParams(category);
    this.heartbeatPartitionManager = heartbeatPartitionManager;
    this.perpetualLockManager = perpetualLockManager;
    this.category = category;
    this.partitionHelper = new PartitionHelper(category, myInstanceId)
        .withPerpetualLockManager(perpetualLockManager);
    this.tenantUtil = tenantUtil;
  }

  public void setPartitionReservationSeconds(int partitionReservationSeconds) {
    if (partitionReservationSeconds <= 0) {
      throw new IllegalArgumentException(
          String.format("Invalid partition reservation of %d seconds", partitionReservationSeconds));
    }
    this.partitionReservationSeconds = partitionReservationSeconds;
  }

  @Override
  public void start() {
    if (disableForTesting) {
      return;
    }
    cleanupExpiredLocks();
    if (tenantUtil.isMultiTenant() && !tenantUtil.isMtiqBatchMode()) {
      return;
    }
    heartbeatPartitionManager.start(myInstanceId, category, partitionReservationSeconds);
  }

  @Override
  public void stop() {
    if (disableForTesting) {
      return;
    }
    if (tenantUtil.isMultiTenant() && !tenantUtil.isMtiqBatchMode()) {
      return;
    }
    heartbeatPartitionManager.stop();
    perpetualLockManager.releasePerpetualLocksForOwner(myInstanceId);
  }

  /**
   * tell the partition helper to do an analysis on the next cycle instead of waiting for the next cycle
   */
  @VisibleForTesting
  public void rebalance() {
    partitionHelper.resetPartitionAnalysis();
  }

  /**
   * There are two steps in getting permission to use the given partition:
   * 1 - checking to see if the partition is either available or already in use by this instance
   * 2 - actually getting the perpetual lock for the partition
   *
   * @return true if we can get the lock, false otherwise
   */
  protected synchronized boolean canUsePartition(String partitionKey) {
    checkPartitionKey(partitionKey);

    return TenantThreadLocal.runAsGlobal(() -> {
      boolean result = false;
      if (partitionHelper.canTryToUsePartition(partitionKey)) {
        if (partitionHelper.isReservationValid(partitionKey)) {
          partitionHelper.setPartitionReservationOutcome(partitionKey, true);
          return true;
        }
        result = tryToReservePartition(partitionKey);
        if (result) {
          partitionHelper.cacheReservation(partitionKey, partitionReservationSeconds);
          if (log.isDebugEnabled()) {
            log.debug("partition {} reserved", partitionKey.substring(0, 5));
          }
        }
        else {
          if (log.isDebugEnabled()) {
            log.debug("unable to reserve partition {}", partitionKey.substring(0, 5));
          }
        }
        partitionHelper.setPartitionReservationOutcome(partitionKey, result);
      }
      else {
        if (log.isDebugEnabled()) {
          log.debug("partition {} is unavailable to this instance", partitionKey.substring(0, 5));
        }
      }

      return result;
    });
  }

  public String getInstanceId() {
    return myInstanceId;
  }

  protected Set<String> getActiveInstanceIds() {
    return partitionHelper.getActiveInstanceIds();
  }

  private void checkPartitionKey(String partitionKey) {
    if (StringUtils.isBlank(partitionKey)) {
      throw new IllegalArgumentException("partitionKey cannot be blank");
    }
  }

  private void cleanupExpiredLocks() {
    TenantThreadLocal.runAsGlobal(() -> {
      perpetualLockManager.removeExpiredLocks();
      return null;
    });
  }

  private boolean tryToReservePartition(String partitionKey) {
    return TenantThreadLocal.runAsGlobal(
        () -> perpetualLockManager.tryAcquireLock(partitionKey, category, myInstanceId, partitionReservationSeconds));
  }

  private void validateInitParams(String category) {
    if (StringUtils.isBlank(category)) {
      throw new IllegalArgumentException(
          String.format("Invalid load balancer configuration: { category: %s }", category));
    }
  }
}
