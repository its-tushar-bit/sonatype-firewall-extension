/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.git.SourceControlInstanceManager;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;

/**
 * This orchestrator is the central hub for event processing and acts sort of like a router for events.  This class
 * does not directly process events, but rather pushes events to the classes that do.
 *
 * This class also implements the event processing solution for multi-node IQ, in conjunction with the
 * #SourceControlInstanceManager. Currently, only one instance of IQ is allowed to process events.  Any instance
 * can create events.
 *
 * The event flow looks like this:
 * - the #SourceControlEventPublisher receives the new event requests
 * - if this instance of IQ is the one processing events the publisher will tag that event with the instance ID
 * - otherwise, the event will go untagged
 * - in either case, the publisher persists the new event to the DB
 * - this class receives the new events from the publisher via a listener interface (needed to break a cyclic
 * dependency)
 * - if this IQ instance is processing events this class will forward the event to the appropriate
 * #UserEventManager, otherwise it will ignore the events
 * - the UserEventManager controls the priority and sequence of events for processing
 * - the UserEventManager is also the first level of parallelization for event processing as the SCM systems
 * generally allow requests from different users to be sent in parallel
 * - also, if this instance is processing events, this class regularly polls the database to get the untagged events
 * coming from other instances of IQ and sends them to the UserEventManagers for processing
 */
@Named
@Singleton
public class SourceControlEventOrchestrator
    implements Managed, SourceControlEventCreationListener
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventOrchestrator.class);

  // arbitrarily picking 2 minutes to detect when another instance of IQ server has gone down/offline and is no longer
  // processing events
  private static final int STALE_EVENT_CUTOFF_MS = 1_000 * 120;

  private final Map<String, UserEventManager> userEventManagerMap = new HashMap<>();

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlEventProcessor sourceControlEventProcessor;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final SourceControlInstanceManager sourceControlInstanceManager;

  private final SourceControlUtils sourceControlUtils;

  private final IqForScmLicenseChecker licenseChecker;

  private ScheduledExecutorService scheduledExecutorService;

  private int otherInstanceEventProcessingIntervalSeconds = 15;

  private int otherInstanceEventProcessingStartupDelaySeconds = 30;

  public boolean disableForTesting;

  @Inject
  public SourceControlEventOrchestrator(
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlEventProcessor sourceControlEventProcessor,
      SourceControlEventPublisher sourceControlEventPublisher,
      SourceControlInstanceManager sourceControlInstanceManager,
      IqForScmLicenseChecker licenseChecker,
      SourceControlUtils sourceControlUtils)
  {
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlEventProcessor = sourceControlEventProcessor;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.sourceControlInstanceManager = sourceControlInstanceManager;
    this.licenseChecker = licenseChecker;
    this.sourceControlUtils = sourceControlUtils;
  }

  /**
   * all new events originating from this instance of IQ will flow in thru here
   */
  @Override
  public void onNewEvent(SourceControlEvent event) {
    synchronized (userEventManagerMap) {
      if (sourceControlInstanceManager.canProcessEvents()) {
        assignEventForProcessing(event);
      }
    }
  }

  @Override
  public void start() {
    if (!disableForTesting) {
      sourceControlEventPublisher.setSourceControlEventListener(this);
      startEventProcessingExecutorService();
    }
  }

  @Override
  public void stop() {
    synchronized (userEventManagerMap) {
      if (null != scheduledExecutorService) {
        scheduledExecutorService.shutdown();
        scheduledExecutorService = null;
      }
      userEventManagerMap.forEach((user, userEventManager) -> userEventManager.stop());
      sourceControlEventProcessor.shutdown();
    }
  }

  @VisibleForTesting
  String getInstanceId() {
    return sourceControlInstanceManager.getSourceControlInstanceId();
  }

  private void assignEventForProcessing(SourceControlEvent event) {
    log.debug("Routing event '{}' for application {} for processing", event.getEventType(), event.getApplicationId());
    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
    UserEventManager userEventManager = userEventManagerMap.computeIfAbsent(event.getScmUsername(),
        k -> new UserEventManager(sourceControlEventDAO, sourceControlEventProcessor, gitRepositoryInfo.getProvider(),
            sourceControlUtils));
    userEventManager.addEvent(event);
  }

  /**
   * Events originating from other running IQ instances wind up in the database 'untagged' with an instance ID.
   * This method:
   * - resets (un-tags) events assigned to other IQ instances that no longer appear to be running
   * - fetches all untagged new events and routes them to the appropriate UserEventManager for processing
   */
  private void fetchAndRouteEvents() {
    if (licenseChecker.isIqForScmSupported()) {
      synchronized (userEventManagerMap) {
        sourceControlEventDAO.resetStaleEvents(new Date(currentTimeMillis() - STALE_EVENT_CUTOFF_MS), getInstanceId());
        List<SourceControlEvent> sourceControlEvents =
            sourceControlEventDAO.selectUnassignedNewEventsAndAssignToInstance(getInstanceId());
        if (CollectionUtils.isNotEmpty(sourceControlEvents)) {
          sourceControlEvents.forEach(this::assignEventForProcessing);
        }
        log.debug(
            "Fetched and routed {} events originating from other IQ instances for processing by this instance '{}'",
            sourceControlEvents.size(), getInstanceId());
        notifyRoutingComplete();
      }
    }
  }

  @VisibleForTesting
  void notifyRoutingComplete() {
    // tests will 'spy' on this method to know when the scheduled event routing has finished
  }

  private void startEventProcessingExecutorService() {
    Runnable sourceControlEventProcessingTask = new SystemRunnable(() -> {
      if (sourceControlInstanceManager.canProcessEvents()) {
        fetchAndRouteEvents();
      }
    });
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("SourceControlEventOrchestrator-%d").setDaemon(true).build();
    scheduledExecutorService = new TenantScheduledThreadPoolExecutor(1, threadFactory);
    scheduledExecutorService.scheduleAtFixedRate(sourceControlEventProcessingTask,
        otherInstanceEventProcessingStartupDelaySeconds, otherInstanceEventProcessingIntervalSeconds,
        TimeUnit.SECONDS);
    log.info("Scheduled possible processing of events coming from other IQ instances to run every {} second(s) " +
            "starting in {} second(s)",
        otherInstanceEventProcessingIntervalSeconds, otherInstanceEventProcessingStartupDelaySeconds);
  }

  @VisibleForTesting
  void setEventProcessingScheduleTimesForTesting(
      int startupDelaySeconds,
      int intervalSeconds)
  {
    otherInstanceEventProcessingStartupDelaySeconds = startupDelaySeconds;
    otherInstanceEventProcessingIntervalSeconds = intervalSeconds;
  }
}
