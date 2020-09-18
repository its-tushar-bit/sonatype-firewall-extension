/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.lifecycle.Managed;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the heartbeat of source control event processing.  It regularly initiates the process that pulls
 * source control events from a durable queue (i.e. DB table) and submits them for execution.  This allows us to
 * better control the amount of work we submit to the system at any given time as load testing revealed
 * that the previous implementation could submit so much work that the system becomes overwhelmed.
 */
@Named
@Singleton
public class SourceControlEventProcessingScheduler
    implements Managed, Job
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventProcessingScheduler.class);

  @VisibleForTesting
  static final int SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS = 15;

  @VisibleForTesting
  static final String NAME = "SourceControlEventProcessing";

  private final SourceControlEventService sourceControlEventService;

  private final ProductLicense productLicense;

  private final InsightConfig insightConfig;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public SourceControlEventProcessingScheduler(
      SourceControlEventService sourceControlEventService,
      ProductLicense productLicense,
      InsightConfig insightConfig,
      TaskScheduler taskScheduler)
  {
    this.sourceControlEventService = sourceControlEventService;
    this.productLicense = productLicense;
    this.insightConfig = insightConfig;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void start() throws Exception {
    if (insightConfig.isFeatureEnabled(Feature.PR_COMMENTING)) {
      startSourceControlEventProcessing();
    }
    else {
      log.info("Pull request commenting feature is disabled; Source control event scheduler is not started.");
    }
  }

  @Override
  public void stop() {
    // noop
  }

  private void startSourceControlEventProcessing() {
    if (disableForTesting) {
      return;
    }

    taskScheduler.schedulePeriodicTask(SourceControlEventProcessingScheduler.class, NAME,
        Duration.ofSeconds(SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS));
    log.info("Scheduled processing of source control events every {} second(s)",
        SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS);
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      processSourceControlEvents();
    }
    catch (Exception e) {
      log.error("Source control event processing error: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  // Visible for testing
  void processSourceControlEvents() {
    if (checkLicense()) {
      log.debug("Commencing source control event processing cycle");

      try {
        int eventCount = sourceControlEventService.processEvents();
        log.debug("{} source control events submitted for execution", eventCount);
      }
      catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      log.debug("Source control event processing cycle complete");
    }
  }

  private boolean checkLicense() {
    return productLicense.hasFeature(LicensedFeature.AUTOMATION);
  }
}
