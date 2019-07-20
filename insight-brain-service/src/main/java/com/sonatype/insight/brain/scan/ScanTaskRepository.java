/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and stores {@link ScanTask}.
 * 
 * @since 1.8
 */
@Named
@Singleton
public class ScanTaskRepository
{
  private static final Logger log = LoggerFactory.getLogger(ScanTaskRepository.class);

  private final Provider<ScanTask> scanTaskProvider;

  private final Map<String, ScanTask> scanTasks;

  private final ThreadPoolExecutor executor;

  private volatile long lastPurge;

  @Inject
  public ScanTaskRepository(Provider<ScanTask> scanTaskProvider) {
    this.scanTaskProvider = scanTaskProvider;
    scanTasks = new ConcurrentHashMap<>();
    executor = new ThreadPoolExecutor(2, 2, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ScanTask-%s").build());
    executor.allowCoreThreadTimeOut(true);
  }

  int getUnfinishedTaskCount() {
    return executor.getActiveCount() + executor.getQueue().size();
  }

  /**
   * Creates a new scan task and schedules it for execution.
   */
  public ScanTask newScanTask(Application app, File binFile, String filename, Stage stage, boolean sendNotifications) {
    ScanTask scanTask = scanTaskProvider.get();
    scanTask.init(app, binFile, filename, stage, sendNotifications);
    scanTasks.put(scanTask.getId(), scanTask);
    log.debug("Scheduling scan task {}", scanTask.getId());
    AuditData.get().continueAsync(new SystemRunnable(scanTask), executor::submit);
    return scanTask;
  }

  /**
   * @throws NotFoundException if there is no ticket for the given taskId
   */
  public ScanTask getByIdNotNull(String id) throws NotFoundException {
    purgeObsoleteTasks();

    ScanTask task = scanTasks.get(id);

    if (task == null) {
      throw new NotFoundException("Cannot find ScanTicket with ID " + id + ".");
    }

    return task;
  }

  /**
   * Removes the task from storage. Does not halt task execution.
   */
  public void remove(String ticketId) {
    log.debug("Removing scan task {}", ticketId);
    scanTasks.remove(ticketId);
  }

  private void purgeObsoleteTasks() {
    long now = System.currentTimeMillis();
    if (now - lastPurge < TimeUnit.SECONDS.toMillis(10)) {
      return;
    }
    lastPurge = now;
    for (Iterator<ScanTask> it = scanTasks.values().iterator(); it.hasNext();) {
      ScanTask task = it.next();
      if (task.isObsolete()) {
        log.debug("Purging scan task {}", task.getId());
        it.remove();
      }
    }
  }
}
