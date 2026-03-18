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
        new SystemRunnable(() -> reserveHeartbeatLock(instanceId, category, partitionReservationSeconds));

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
}
