/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.sonatype.insight.brain.dataaccess.consumption.ConsumptionEventDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ConsumptionRecorder
{
  private static final Logger log = LoggerFactory.getLogger(ConsumptionRecorder.class);

  private static final int CORE_POOL_SIZE_PER_TENANT = 1;

  private static final int MAX_POOL_SIZE_PER_TENANT = 1;

  private static final long KEEP_ALIVE_SECONDS = 5L;

  private static final int QUEUE_CAPACITY_PER_TENANT = 10_000;

  private final ConsumptionEventDAO dao;

  private final TenantReference<TenantThreadPoolExecutor> executors;

  private final TenantReference<AtomicLong> droppedPerTenant = new TenantReference<>(AtomicLong::new);

  @Inject
  public ConsumptionRecorder(final ConsumptionEventDAO dao, final ShutdownHandler shutdownHandler) {
    this.dao = dao;
    ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("consumption-recorder-%d")
        .build();
    this.executors = new TenantReference<>(() -> {
      TenantThreadPoolExecutor executor = new TenantThreadPoolExecutor(
          CORE_POOL_SIZE_PER_TENANT,
          MAX_POOL_SIZE_PER_TENANT,
          KEEP_ALIVE_SECONDS,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_TENANT),
          threadFactory,
          new AbortPolicy(),
          "consumption-recorder",
          "ConsumptionRecorder");
      executor.allowCoreThreadTimeOut(true);
      shutdownHandler.add(executor);
      return executor;
    });
  }

  public void record(ConsumptionEvent event) {
    if (!SystemConfigurationPropertyFeature.CONSUMPTION_REPORTING.isEnabled()) {
      return;
    }
    if (event.getIdempotencyKey() == null) {
      // Background recorders (ScanPolicyEvaluator, PullRequestRemediationService) build
      // the event from their own state and do not populate the request-thread's
      // ConsumptionContext. Build a detached context that combines the event's
      // userId/appId/scanId with whatever sessionId is on the threadlocal — this avoids
      // mutating the request-scoped ctx (which would corrupt subsequent record() calls
      // on the same thread for a different scan/app).
      String key = IdempotencyKeyGenerator.generate(
          event.getActivityType(),
          mergedContext(event),
          /* entityId */ null);
      if (key != null) {
        event.setIdempotencyKey(key);
      }
    }
    try {
      executors.get().submit(() -> writeEvent(event));
    }
    catch (RejectedExecutionException e) {
      long totalDropped = droppedPerTenant.get().incrementAndGet();
      log.warn(
          "Consumption recording queue full (capacity={}, totalDroppedForTenant={}); "
              + "event dropped for org={} app={} scan={} activity={} source={} count={}",
          QUEUE_CAPACITY_PER_TENANT, totalDropped,
          event.getOrgId(), event.getAppId(), event.getScanId(),
          event.getActivityType(), event.getSource(), event.getComponentCount());
    }
    catch (Exception e) {
      log.warn("Failed to enqueue consumption event for org={}", event.getOrgId(), e);
    }
  }

  /**
   * Build a detached {@link ConsumptionContext} that prefers the event's own
   * userId/appId/scanId (the call site that built the event has more reliable
   * knowledge of these than the threadlocal does), falling back to whatever
   * the threadlocal carries. The sessionId always comes from the threadlocal
   * because it is set elsewhere (HdsClient.emitEvent or ConsumptionContextFilter)
   * and is not present on the event.
   *
   * <p>
   * Returns {@code null} when neither source can supply a userId — without that,
   * IdempotencyKeyGenerator will not produce a key for any activity type.
   */
  @Nullable
  private static ConsumptionContext mergedContext(ConsumptionEvent event) {
    ConsumptionContext threadCtx = ConsumptionContext.get();
    String userId = preferEventThenCtx(event.getUserId(), threadCtx == null ? null : threadCtx.getUserId());
    if (userId == null) {
      return null;
    }
    String appId = preferEventThenCtx(event.getAppId(), threadCtx == null ? null : threadCtx.getAppId());
    String scanId = preferEventThenCtx(event.getScanId(), threadCtx == null ? null : threadCtx.getScanId());
    String sessionId = threadCtx == null ? null : threadCtx.getSessionId();
    return ConsumptionContext.detached(userId, appId, scanId, sessionId);
  }

  private static String preferEventThenCtx(@Nullable String fromEvent, @Nullable String fromCtx) {
    return fromEvent != null ? fromEvent : fromCtx;
  }

  public long getDroppedEventCount() {
    AtomicLong counter = droppedPerTenant.get();
    return counter == null ? 0L : counter.get();
  }

  public int getQueueDepth() {
    TenantThreadPoolExecutor executor = executors.get();
    return executor == null ? 0 : executor.getQueue().size();
  }

  TenantReference<TenantThreadPoolExecutor> getExecutors() {
    return executors;
  }

  private void writeEvent(ConsumptionEvent event) {
    try {
      dao.recordEvent(event);
    }
    catch (Exception e) {
      log.warn("Failed to write consumption event for org={}", event.getOrgId(), e);
    }
  }
}
