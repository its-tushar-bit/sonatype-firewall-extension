/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.waiver;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.service.InsightJob;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@DisallowConcurrentExecution
public class WaivedComponentUpgradeTask
    extends Task
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(WaivedComponentUpgradeTask.class);

  WaivedComponentUpgradeInspector waivedComponentUpgradeInspector;

  @Inject
  public WaivedComponentUpgradeTask(WaivedComponentUpgradeInspector waivedComponentUpgradeInspector) {
    super("triggerWaivedComponentUpgradeInspector");
    this.waivedComponentUpgradeInspector = waivedComponentUpgradeInspector;
  }

  @Override
  public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
    log.info("Automatic request to run Waived Component Upgrade Inspector");
    execute(waivedComponentUpgradeInspector, log, "Waived Component Upgrade Inspector error");
    log.info("Next Waived Component Upgrade Inspector execution scheduled for {}",
        jobExecutionContext.getNextFireTime());
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    log.info("Manual request to run Waived Component Upgrade Inspector");
    waivedComponentUpgradeInspector.run();
    printWriter.write("Completed manual Waived Component Upgrade Inspector execution\n");
  }

  @Override
  public String getJobName() {
    return WaivedComponentUpgradeTask.class.getSimpleName();
  }
}
