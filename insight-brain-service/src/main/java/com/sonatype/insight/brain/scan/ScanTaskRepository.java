/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Creates and stores {@link ScanTask}.
 * 
 * @since 1.8
 */
@Named
public class ScanTaskRepository
{
  private final Provider<ScanTask> scanTaskProvider;

  private final Map<String, ScanTask> scanTasks;

  private final ThreadPoolExecutor executor;

  @Inject
  public ScanTaskRepository(Provider<ScanTask> scanTaskProvider) {
    this.scanTaskProvider = scanTaskProvider;
    scanTasks = new ConcurrentHashMap<>();
    executor = new ThreadPoolExecutor(1, 2, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ScanTask-%s").build());
    executor.allowCoreThreadTimeOut(true);
  }

  /**
   * Creates a new scan task and schedules it for execution.
   */
  public ScanTask newScanTask(Application app, File binFile, Stage stage, boolean sendNotifications) {
    ScanTask scanTask = scanTaskProvider.get();
    scanTask.init(app, binFile, stage, sendNotifications);
    scanTasks.put(scanTask.getId(), scanTask);
    executor.submit(scanTask);
    return scanTask;
  }

  /**
   * @throws NotFoundException if there is no ticket for the given taskId
   */
  public ScanTask getByIdNotNull(String id) throws NotFoundException {
    ScanTask task = scanTasks.get(id);

    if (task == null) {
      throw new NotFoundException("Cannot find ScanTicket with id " + id + ".");
    }

    return task;
  }

  /**
   * Removes the task from storage. Does not halt task execution.
   */
  public void remove(String ticketId) {
    scanTasks.remove(ticketId);
  }
}
