/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.UUID;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;

/**
 * At this moment all SCM operations are pinned to a single IQ instance.  In the future the plan is to distribute
 * this work across multiple instances in a clustered environment.
 *
 * As such, this class is pretty simple to start with but will expand based on how the work is to be distributed.
 * - initially we could pin an IQ instance to a particular user/token
 * - eventually we want to leverage the full capabilities of clustered IQ and let any instance pick up any work
 * that doesn't conflict with the work another instance is doing
 */
@Named
@Singleton
public class SourceControlInstanceManager
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlInstanceManager.class);

  public static final String SOURCE_CONTROL_ACCESS_LOCK = "source-control-access-c78943f1";

  // must be greater than the polling interval so that the instance with the lock doesn't lose it before it
  // has a chance to refresh it
  private int instanceLockReservationTimeSeconds =
      PullRequestPollingScheduler.PULL_REQUEST_DISCOVERY_INTERVAL_SECONDS + 5;

  private final PerpetualLockManager perpetualLockManager = new PerpetualLockManager();

  private Boolean hasInstanceLock;

  // must be lower than the polling interval - we want it to expire before the next polling interval so that
  // it can be continually renewed
  private int instanceLockCacheExpirationSeconds = instanceLockReservationTimeSeconds - 5;

  private long instanceLockCacheExpirationTime = currentTimeMillis();

  // non-static for testing purposes
  private final String sourceControlInstanceId;

  public SourceControlInstanceManager() {
    sourceControlInstanceId = UUID.randomUUID().toString();
    log.info("Created SourceControlInstanceManager with instance ID {}", sourceControlInstanceId);
  }

  public String getSourceControlInstanceId() {
    return sourceControlInstanceId;
  }

  public boolean canPoll() {
    return tryReserveInstanceLockWithCaching();
  }

  public boolean canProcessEvents() {
    return tryReserveInstanceLockWithCaching();
  }

  private synchronized boolean tryReserveInstanceLockWithCaching() {
    if (hasInstanceLockCacheExpired()) {
      hasInstanceLock = tryReserveInstanceLock();
      updateInstanceLockCacheExpirationTime();
    }
    return hasInstanceLock;
  }

  private boolean hasInstanceLockCacheExpired() {
    return null == hasInstanceLock || instanceLockCacheExpirationTime <= currentTimeMillis();
  }

  private boolean tryReserveInstanceLock() {
    return TenantThreadLocal.runAsGlobal(() -> perpetualLockManager
        .tryAcquireLock(SOURCE_CONTROL_ACCESS_LOCK, sourceControlInstanceId, instanceLockReservationTimeSeconds));
  }

  private void updateInstanceLockCacheExpirationTime() {
    instanceLockCacheExpirationTime = currentTimeMillis() + instanceLockCacheExpirationSeconds * 1_000;
  }

  @VisibleForTesting
  synchronized void releaseInstance() {
    TenantThreadLocal.runAsGlobal(() -> {
      perpetualLockManager.releasePerpetualLock(SOURCE_CONTROL_ACCESS_LOCK, sourceControlInstanceId);
      return null;
    });
    hasInstanceLock = null;
  }

  @VisibleForTesting
  SourceControlInstanceManager setInstanceLockReservationSecondsForTesting(int reservationSeconds) {
    instanceLockReservationTimeSeconds = reservationSeconds;
    return this;
  }

  @VisibleForTesting
  SourceControlInstanceManager setInstanceLockCacheExpirationForTesting(int cacheExpirationSeconds) {
    instanceLockCacheExpirationSeconds = cacheExpirationSeconds;
    return this;
  }
}
