/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.utils.DateUtils;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;

/**
 * This executor spreads out the start times or default monitoring for all tenants. The determines what time of the day
 * a given tenant should start by evenly dividing up the day by hte number of tenants currently registered.
 * Note: that this does <b>not</b> take into account tenants that are not configured to run default branch monitoring.
 */
@Named
@Singleton
@Primary
public class MultiTenantDefaultBranchMonitorExecutor
    extends BranchMonitorExecutor
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantDefaultBranchMonitorExecutor.class);

  private static final int INTERVAL_IN_HOURS = 24;

  private static final int INTERVAL_IN_MINUTES = INTERVAL_IN_HOURS * 60;

  private final TaskScheduler taskScheduler;

  private final TenantService tenantService;

  @Inject
  public MultiTenantDefaultBranchMonitorExecutor(
      TaskScheduler taskScheduler,
      SourceControlDAO sourceControlDAO,
      SourceControlEventPublisher sourceControlEventPublisher,
      TenantService tenantService)
  {
    super(sourceControlDAO, sourceControlEventPublisher);
    this.taskScheduler = taskScheduler;
    this.tenantService = tenantService;
  }

  @Override
  public void schedule(InsightJob job) {
    List<String> allTenantsNames = tenantService.getAllTenantsNames();

    String tenantSlug = tenantService.getTenantSlug();
    if (!allTenantsNames.contains(tenantSlug)) {
      log.error("{} is not a valid tenant", tenantSlug);
      return;
    }

    LocalDateTime date = calculateStartTime(tenantSlug, allTenantsNames);
    log.info("scheduling default branch monitoring to run at {}", date);

    taskScheduler.scheduleOneTimeTask(job, date);
  }

  @VisibleForTesting
  LocalDateTime calculateStartTime(String tenantSlug, List<String> allTenantNames) {
    Collections.sort(allTenantNames);
    int tenantIndex = allTenantNames.indexOf(tenantSlug);
    int numberOfTenants = allTenantNames.size();

    int offset = (INTERVAL_IN_MINUTES / numberOfTenants) * tenantIndex;

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = now.with(LocalTime.MIN).plusMinutes(offset);

    return DateUtils.getClosestFutureDateTime(now, startTime, INTERVAL_IN_HOURS);
  }

  @Override
  public void performScan(InsightJob job) {
    updateDefaultBranchScans(INTERVAL_IN_MINUTES);
    schedule(job);
  }
}
