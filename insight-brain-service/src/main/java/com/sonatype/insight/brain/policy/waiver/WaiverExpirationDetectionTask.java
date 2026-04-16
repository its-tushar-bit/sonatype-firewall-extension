/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a scheduled or manually triggered Waiver Expiration Detection task.
 * This task detects recently expired waivers and emits webhook events.
 *
 * The @DisallowConcurrentExecution annotation prevents Quartz from running two jobs with the
 * same Quartz job key concurrently. In MTIQ, this allows parallel execution per tenant since
 * each tenant has a different job key.
 *
 * @since 1.178.0
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class WaiverExpirationDetectionTask
    extends Task
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(WaiverExpirationDetectionTask.class);

  private Provider<WaiverExpirationDetectionService> waiverExpirationDetectionServiceProvider;

  @Inject
  public WaiverExpirationDetectionTask(
      Provider<WaiverExpirationDetectionService> waiverExpirationDetectionServiceProvider)
  {
    super("triggerWaiverExpirationDetection");
    this.waiverExpirationDetectionServiceProvider = waiverExpirationDetectionServiceProvider;
  }

  @Override
  public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Automatic request to run Waiver Expiration Detection for tenant {}",
        TenantThreadLocal.getTenant());
    execute(waiverExpirationDetectionServiceProvider.get(), log, "Waiver Expiration Detection error");
    log.info("Next Waiver Expiration Detection execution scheduled for {}",
        jobExecutionContext.getNextFireTime());
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    log.info("Manual request to run Waiver Expiration Detection");
    waiverExpirationDetectionServiceProvider.get().run();
    printWriter.write("Completed manual Waiver Expiration Detection execution\n");
  }

  @Override
  public String getJobName() {
    return WaiverExpirationDetectionTask.class.getSimpleName();
  }
}
