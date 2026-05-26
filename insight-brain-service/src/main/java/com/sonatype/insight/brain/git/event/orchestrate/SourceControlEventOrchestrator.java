/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.OneTimeSystemRunnable;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer.SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS;

/**
 * This orchestrator is the central hub for event processing and acts sort of like a router for events. This class
 * does not directly process events, but rather pushes events to the classes that do.
 *
 * This class also implements the event processing solution for multi-node IQ, in conjunction with the
 * #SourceControlInstanceManager. Currently, only one instance of IQ is allowed to process events. Any instance
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
    implements TenantManaged, SourceControlEventCreationListener
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventOrchestrator.class);

  private static final int DEFAULT_EVENT_PROCESSING_STARTUP_DELAY_SECONDS = 30;

  private final TenantReference<Map<String, UserEventManager>> userEventManagerMap =
      new TenantReference<>(HashMap::new);

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlEventProcessor sourceControlEventProcessor;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final SourceControlLoadBalancer sourceControlLoadBalancer;

  private final SourceControlUtils sourceControlUtils;

  private final IqForScmLicenseChecker licenseChecker;

  private final ApiConfigFeaturesService apiConfigFeaturesService;

  private final TenantReference<ScheduledExecutorService> tenantScheduledExecutorServices;

  private final ShutdownHandler shutdownHandler;

  private final ScmNodeProcessor scmNodeProcessor;

  private int otherInstanceEventProcessingIntervalSeconds = SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS;

  private int otherInstanceEventProcessingStartupDelaySeconds = DEFAULT_EVENT_PROCESSING_STARTUP_DELAY_SECONDS;

  public boolean disableForTesting;

  @Inject
  public SourceControlEventOrchestrator(
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlEventProcessor sourceControlEventProcessor,
      SourceControlEventPublisher sourceControlEventPublisher,
      SourceControlLoadBalancer sourceControlLoadBalancer,
      IqForScmLicenseChecker licenseChecker,
      SourceControlUtils sourceControlUtils,
      ApiConfigFeaturesService apiConfigFeaturesService,
      ShutdownHandler shutdownHandler,
      ScmNodeProcessor scmNodeProcessor)
  {
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlEventProcessor = sourceControlEventProcessor;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.sourceControlLoadBalancer = sourceControlLoadBalancer;
    this.licenseChecker = licenseChecker;
    this.sourceControlUtils = sourceControlUtils;
    this.apiConfigFeaturesService = apiConfigFeaturesService;
    this.tenantScheduledExecutorServices = new TenantReference<>(this::newExecutor);
    this.shutdownHandler = shutdownHandler;
    this.scmNodeProcessor = scmNodeProcessor;
  }

  // Visible for testing
  ScheduledExecutorService newExecutor() {
    String threadNameFormat = String.format("SourceControlEventOrchestrator_%s",
        new TenantUtil().getTenantSlugForSynchronization());

    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat(threadNameFormat + "-%d").setDaemon(true).build();
    TenantScheduledThreadPoolExecutor tenantScheduledThreadPoolExecutor =
        new TenantScheduledThreadPoolExecutor(1, threadFactory);
    shutdownHandler.add(tenantScheduledThreadPoolExecutor);
    return tenantScheduledThreadPoolExecutor;
  }

  /**
   * all new events originating from this instance of IQ will flow in thru here
   */
  @Override
  public void onNewEvent(SourceControlEvent event) {
    if (disableForTesting) {
      return;
    }
    synchronized (userEventManagerMap.get()) {
      if (sourceControlLoadBalancer.reserveEvent(event)) {
        assignEventForProcessing(event);
      }
    }
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    if (scmNodeProcessor.shouldRun()) {
      sourceControlEventPublisher.setSourceControlEventListener(this);
      startEventProcessingExecutorService();
    }
  }

  @Override
  public void deregister() {
    synchronized (userEventManagerMap.get()) {
      if (scmNodeProcessor.shouldRun()) {
        if (null != tenantScheduledExecutorServices.get()) {
          tenantScheduledExecutorServices.get().shutdown();
          notifyExecutorShutdown();
        }
        userEventManagerMap.get().forEach((user, userEventManager) -> userEventManager.stop());
      }
    }
  }

  @VisibleForTesting
  void notifyExecutorShutdown() {
    // noop
  }

  @VisibleForTesting
  String getInstanceId() {
    return sourceControlLoadBalancer.getInstanceId();
  }

  private void assignEventForProcessing(SourceControlEvent event) {
    log.debug("Routing event '{}' for application {} for processing", event.getEventType(), event.getApplicationId());
    GitRepositoryInfo gitRepositoryInfo =
        sourceControlUtils.getGitRepositoryInfoForApplication(event.getApplicationId());
    UserEventManager userEventManager = userEventManagerMap.get()
        .computeIfAbsent(event.getScmUsername(),
            k -> new UserEventManager(
                sourceControlEventDAO,
                sourceControlLoadBalancer,
                sourceControlEventProcessor,
                gitRepositoryInfo.getProvider(),
                sourceControlUtils,
                shutdownHandler));
    userEventManager.addEvent(event);
  }

  /**
   * Fetches events that can be processed by this instance of IQ Server and routes them to the appropriate
   * UserEventManager
   */
  private void fetchAndRouteEvents() {
    if (disableForTesting) {
      return;
    }
    if (!licenseChecker.isIqForScmSupported() || !apiConfigFeaturesService.isSaasLifecycleScmEnabled()) {
      log.trace("unable to fetch and route source control events due to licensing or configuration");
      return;
    }

    synchronized (userEventManagerMap.get()) {
      List<SourceControlEvent> sourceControlEvents = sourceControlLoadBalancer.acquireEventsToProcess();
      if (CollectionUtils.isNotEmpty(sourceControlEvents)) {
        for (SourceControlEvent event : sourceControlEvents) {
          assignEventForProcessing(event);
        }
      }
      log.debug(
          "Fetched and routed {} events originating from other IQ instances for processing by this instance '{}'",
          sourceControlEvents.size(), getInstanceId());
      notifyRoutingComplete();
    }
  }

  @VisibleForTesting
  void notifyRoutingComplete() {
    // tests will 'spy' on this method to know when the scheduled event routing has finished
  }

  @VisibleForTesting
  void startEventProcessingExecutorService() {
    ScheduledExecutorService scheduledExecutorService = tenantScheduledExecutorServices.get();
    Runnable sourceControlEventProcessingTask = () -> {
      OneTimeSystemRunnable oneTimeSystemRunnable = new OneTimeSystemRunnable(this::fetchAndRouteEvents);
      try {
        oneTimeSystemRunnable.run();
      }
      catch (Exception e) {
        log.error("error trying to process source control events", e);
      }
    };

    scheduledExecutorService.scheduleAtFixedRate(sourceControlEventProcessingTask,
        otherInstanceEventProcessingStartupDelaySeconds, otherInstanceEventProcessingIntervalSeconds,
        TimeUnit.SECONDS);
    log.info("Scheduled possible processing of events coming from other IQ instances to run every {} second(s) " +
        "starting in {} second(s)",
        otherInstanceEventProcessingIntervalSeconds, otherInstanceEventProcessingStartupDelaySeconds);
  }

  @VisibleForTesting
  public void setEventProcessingScheduleTimesForTesting(
      int startupDelaySeconds,
      int intervalSeconds)
  {
    otherInstanceEventProcessingStartupDelaySeconds = startupDelaySeconds;
    otherInstanceEventProcessingIntervalSeconds = intervalSeconds;
  }
}
