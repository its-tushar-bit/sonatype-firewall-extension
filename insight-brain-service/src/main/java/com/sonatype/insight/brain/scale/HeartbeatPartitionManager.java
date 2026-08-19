/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scale;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class HeartbeatPartitionManager
{
  private static final Logger log = LoggerFactory.getLogger(HeartbeatPartitionManager.class);

  private static final long MIN_HEARTBEAT_REFRESH_SECONDS = 15L;

  private final PerpetualLockManager perpetualLockManager;

  private String instanceId;

  private ScheduledExecutorService heartbeatExecutorService;

  @Inject
  public HeartbeatPartitionManager(PerpetualLockManager perpetualLockManager) {
    this.perpetualLockManager = perpetualLockManager;
  }

  public void start(final String instanceId, final String category, final long partitionReservationSeconds) {
    this.instanceId = instanceId;
    // the heartbeat task reserves a lock specific to this instance as a way to inform other running IQ instances
    // that this one is up, running and available to participate in the coordination of work between them
    Runnable heartbeatTask =
        new SystemRunnable(createHeartbeatTaskBody(instanceId, category, partitionReservationSeconds));

    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat(category + "SelfThrottlingLoadBalancer-%d")
            .setDaemon(true)
            .build();

    heartbeatExecutorService = new ScheduledThreadPoolExecutor(1, threadFactory);

    long heartbeatRefreshSeconds =
        Math.max(MIN_HEARTBEAT_REFRESH_SECONDS, partitionReservationSeconds - 5L);

    heartbeatExecutorService.scheduleAtFixedRate(
        heartbeatTask, 0, heartbeatRefreshSeconds, TimeUnit.SECONDS);

    log.info(
        "Scheduled {} load balancer heartbeat to run every {} second(s) starting in {} second(s)",
        category, heartbeatRefreshSeconds, 0);
  }

  public void stop() {
    if (null != heartbeatExecutorService) {
      heartbeatExecutorService.shutdown();
      heartbeatExecutorService = null;
      removeHeartbeatLock();
    }
  }

  private void removeHeartbeatLock() {
    if (StringUtils.isNotBlank(instanceId)) {
      TenantThreadLocal.runAsGlobal(() -> {
        perpetualLockManager.removePerpetualLock(instanceId);
        return null;
      });
    }
  }

  // the 'heartbeat lock' represents a perpetual lock where the lock ID and the lock owner are the same value
  private void reserveHeartbeatLock(String instanceId, String category, long partitionReservationSeconds) {
    TenantThreadLocal.runAsGlobal(() -> perpetualLockManager.tryAcquireLock(
        instanceId, category, instanceId, partitionReservationSeconds));
  }

  /**
   * Builds the Runnable that each scheduled heartbeat cycle executes.
   *
   * <p>
   * This body swallows {@link Exception}s so that
   * {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate} does not permanently cancel the
   * heartbeat after a transient failure -- e.g. an Aurora failover producing a one-off connection-closed
   * PSQLException.
   *
   * <p>
   * {@link Error}s are deliberately <em>not</em> caught. Errors signal that the JVM itself is in trouble
   * (OutOfMemoryError, StackOverflowError, LinkageError, etc.); continuing to hit the database every 60 seconds
   * as if nothing happened is unsafe and hides real problems. Letting the Error propagate means the heartbeat
   * task is cancelled by the scheduler -- at which point the instance's lock will expire naturally and other
   * instances will notice the missing heartbeat, which is the correct failure mode for a sick JVM.
   */
  @VisibleForTesting
  Runnable createHeartbeatTaskBody(
      final String instanceId,
      final String category,
      final long partitionReservationSeconds)
  {
    return () -> {
      try {
        reserveHeartbeatLock(instanceId, category, partitionReservationSeconds);
      }
      catch (Exception e) {
        // Transient failure: log and let the next scheduled execution retry. See method javadoc for why
        // this intentionally does NOT catch Error.
        log.warn("Heartbeat renewal failed for {} instance {}; will retry next cycle", category, instanceId, e);
      }
    };
  }
}
