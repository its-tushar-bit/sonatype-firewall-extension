/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ClearRolePermissionCache
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ClearRolePermissionCache.class);

  // Visible for testing
  static final String TASK_NAME = "ClearRolePermissionCache";

  private final TaskScheduler taskScheduler;

  @Inject
  public ClearRolePermissionCache(TaskScheduler taskScheduler) {
    this.taskScheduler = taskScheduler;
  }

  public void scheduleClearRolePermissionCacheForAllOtherNodes() {
    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this);
  }

  @Override
  public void register() {
    RolePermissionDAO.setClearRolePermissionCacheForAllOtherNodes(
        this::scheduleClearRolePermissionCacheForAllOtherNodes);
  }

  // Visible for testing
  void clearRolePermissionCache() {
    RolePermissionDAO.clearRolePermissionCache();
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::clearRolePermissionCache, log, "Failed to clear role permission cache");
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
