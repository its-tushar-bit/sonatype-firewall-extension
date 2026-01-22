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
 * Runs a scheduled or manually triggered Waived Component Upgrade Inspector task.
 * 
 * The @DisallowConcurrentExecution annotation on this class can be confusing/misleading, especially when combined with
 * the @Singleton annotation.
 * Here is how it works:
 * The @DisallowConcurrentExecution annotation is a Quartz annotation that doesn't allow Quartz to run two jobs with the
 * same Quartz job key concurrently. It does not act on java instances. This means we can have a singleton that triggers
 * concurrent jobs (as long as the jobs have different keys).
 * This is particularly important in MTIQ, where Quartz jobs have the tenant slug in their job key, which allows MTIQ to
 * run a job/task of this type per tenant in parallel (despite of the @DisallowConcurrentExecution annotation).
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class WaivedComponentUpgradeTask
    extends Task
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(WaivedComponentUpgradeTask.class);

  private Provider<WaivedComponentUpgradeInspector> waivedComponentUpgradeInspectorProvider;

  @Inject
  public WaivedComponentUpgradeTask(Provider<WaivedComponentUpgradeInspector> waivedComponentUpgradeInspectorProvider) {
    super("triggerWaivedComponentUpgradeInspector");
    this.waivedComponentUpgradeInspectorProvider = waivedComponentUpgradeInspectorProvider;
  }

  @Override
  public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Automatic request to run Waived Component Upgrade Inspector for tenant {}",
        TenantThreadLocal.getTenant());
    execute(waivedComponentUpgradeInspectorProvider.get(), log, "Waived Component Upgrade Inspector error");
    log.info("Next Waived Component Upgrade Inspector execution scheduled for {}",
        jobExecutionContext.getNextFireTime());
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    log.info("Manual request to run Waived Component Upgrade Inspector");
    waivedComponentUpgradeInspectorProvider.get().run();
    printWriter.write("Completed manual Waived Component Upgrade Inspector execution\n");
  }

  @Override
  public String getJobName() {
    return WaivedComponentUpgradeTask.class.getSimpleName();
  }
}
