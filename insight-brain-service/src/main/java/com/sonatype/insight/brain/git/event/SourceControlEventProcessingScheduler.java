/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
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
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventProcessingScheduler.class);

  static final int SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS = 15;

  static final int SOURCE_CONTROL_EVENT_PROCESSING_DELAY_SECONDS = 30;

  @VisibleForTesting
  static final String NAME = "SourceControlEventProcessing";

  private final SourceControlEventService sourceControlEventService;

  private final IqForScmLicenseChecker licenseChecker;

  private final InsightConfig insightConfig;

  private ScheduledExecutorService scheduledExecutorService;

  private final int sourceControlEventProcessingIntervalSeconds;

  private final int sourceControlEventProcessingDelaySeconds;

  public boolean disableForTesting;

  @Inject
  public SourceControlEventProcessingScheduler(
      SourceControlEventService sourceControlEventService,
      InsightConfig insightConfig,
      IqForScmLicenseChecker licenseChecker)
  {
    this(sourceControlEventService, insightConfig, licenseChecker, SOURCE_CONTROL_EVENT_PROCESSING_DELAY_SECONDS,
        SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS);
  }

  @VisibleForTesting
  SourceControlEventProcessingScheduler(
      SourceControlEventService sourceControlEventService,
      InsightConfig insightConfig,
      IqForScmLicenseChecker licenseChecker,
      int sourceControlEventProcessingDelaySeconds,
      int sourceControlEventProcessingIntervalSeconds)
  {
    this.sourceControlEventService = sourceControlEventService;
    this.insightConfig = insightConfig;
    this.licenseChecker = licenseChecker;
    this.sourceControlEventProcessingDelaySeconds = sourceControlEventProcessingDelaySeconds;
    this.sourceControlEventProcessingIntervalSeconds = sourceControlEventProcessingIntervalSeconds;
  }

  @Override
  public void start() throws Exception {
    if (!insightConfig.isExperimentalFeatureEnabled(Feature.ORCHESTRATED_EVENT_PROCESSING)) {
      startSourceControlEventProcessing();
    }
  }

  private void startSourceControlEventProcessing() {
    if (scheduledExecutorService != null || disableForTesting) {
      return;
    }
    scheduledExecutorService = newExecutor();
    Runnable sourceControlEventProcessingTask = new SystemRunnable(() -> {
      try {
        processSourceControlEvents();
      }
      catch (RuntimeException e) {
        log.warn("Failed to process source control events", e);
      }
    });
    scheduledExecutorService
        .scheduleAtFixedRate(sourceControlEventProcessingTask, sourceControlEventProcessingDelaySeconds,
            sourceControlEventProcessingIntervalSeconds, TimeUnit.SECONDS);
    log.info("Scheduled processing of source control events every {} second(s) starting in {} second(s)",
        sourceControlEventProcessingIntervalSeconds, sourceControlEventProcessingDelaySeconds);
  }

  @Override
  public void stop() {
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
      scheduledExecutorService = null;
      log.info("Stopped source control event processing");
    }
  }

  private ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("SourceControlEventProcessing-%d").setDaemon(true).build();
    return new ScheduledThreadPoolExecutor(1, threadFactory);
  }

  // Visible for testing
  void processSourceControlEvents() {
    if (licenseChecker.isIqForScmSupported()) {
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
}
