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
