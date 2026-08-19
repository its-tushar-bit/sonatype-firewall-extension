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
import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the waiver expiration detection job and exposes the legacy admin task path.
 *
 * <p>
 * The public no-arg constructor is retained for legacy Quartz compatibility checks and tests,
 * while the injected constructor is used for normal Spring-managed execution.
 * </p>
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class WaiverExpirationDetectionTask
    extends AdminTask
    implements InsightJob
{
  public static final String PATH = "triggerWaiverExpirationDetection";

  private static final Logger log = LoggerFactory.getLogger(WaiverExpirationDetectionTask.class);

  private Provider<WaiverExpirationDetectionService> waiverExpirationDetectionServiceProvider;

  public WaiverExpirationDetectionTask() {
    super(PATH);
    // Retained for persisted-job compatibility checks and lightweight construction in tests.
  }

  @Inject
  public WaiverExpirationDetectionTask(
      Provider<WaiverExpirationDetectionService> waiverExpirationDetectionServiceProvider)
  {
    super(PATH);
    this.waiverExpirationDetectionServiceProvider = waiverExpirationDetectionServiceProvider;
  }

  @Override
  public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
    WaiverExpirationDetectionService service = getServiceIfAvailable();
    if (service == null) {
      log.info("Skipping legacy WaiverExpirationDetectionTask execution for persisted Quartz compatibility");
      return;
    }

    log.info("Automatic request to run Waiver Expiration Detection for tenant {}",
        TenantThreadLocal.getTenant());
    execute(service, log, "Waiver Expiration Detection error");
    log.info("Next Waiver Expiration Detection execution scheduled for {}",
        jobExecutionContext.getNextFireTime());
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    WaiverExpirationDetectionService service = getServiceIfAvailable();
    if (service == null) {
      log.info("Skipping legacy WaiverExpirationDetectionTask manual execution for persisted Quartz compatibility");
      return;
    }

    log.info("Manual request to run Waiver Expiration Detection");
    service.run();
    output.write("Completed manual Waiver Expiration Detection execution\n");
  }

  private WaiverExpirationDetectionService getServiceIfAvailable() {
    return waiverExpirationDetectionServiceProvider == null ? null : waiverExpirationDetectionServiceProvider.get();
  }

  @Override
  public String getJobName() {
    return WaiverExpirationDetectionTask.class.getSimpleName();
  }
}
