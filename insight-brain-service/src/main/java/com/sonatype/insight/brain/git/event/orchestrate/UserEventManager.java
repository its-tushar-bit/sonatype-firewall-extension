/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.ApplicationScopeEventProcessingSuspensionRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.EventProcessedListener;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.EventProcessingErrorRetryRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.PerformanceThrottlingRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.RepositoryUrlErrorRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.UserScopeEventProcessingSuspensionRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.selection.EventCostSelectionRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.selection.SimultaneousEventSelectionRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.selection.SingleApplicationSelectionRule;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.OneTimeSystemRunnable;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserEventManager
    implements SourceControlEventStatusListener
{
  private static final Logger log = LoggerFactory.getLogger(UserEventManager.class);

  private static final int BACKUP_TRIGGER_INTERVAL_SECONDS = 60;

  private static final int BACKUP_TRIGGER_STARTUP_DELAY_SECONDS = 30;

  private static final int EVENT_PUSH_MAX_QUIET_PERIOD_SECONDS = 15;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlLoadBalancer sourceControlLoadBalancer;

  private final SourceControlEventProcessor sourceControlEventProcessor;

  private final SortedMap<Integer, List<SourceControlEvent>> prioritizedEventMap = new TreeMap<>();

  private final List<SourceControlEvent> retryEventBucket = new ArrayList<>();

  private final Map<String, SourceControlEvent> eventsInProgress = new HashMap<>();

  private final ApplicationScopeEventProcessingSuspensionRule applicationScopeEventProcessingSuspensionRule =
      new ApplicationScopeEventProcessingSuspensionRule();

  private final EventCostSelectionRule eventCostSelectionRule;

  private final EventProcessingErrorRetryRule eventProcessingErrorRetryRule = new EventProcessingErrorRetryRule();

  private final PerformanceThrottlingRule performanceThrottlingRule = new PerformanceThrottlingRule();

  private final RepositoryUrlErrorRule repositoryUrlErrorRule;

  private final SimultaneousEventSelectionRule simultaneousEventSelectionRule;

  private final SingleApplicationSelectionRule singleApplicationSelectionRule = new SingleApplicationSelectionRule();

  private final UserScopeEventProcessingSuspensionRule userScopeEventProcessingSuspensionRule =
      new UserScopeEventProcessingSuspensionRule();

  private final List<EventProcessedListener> eventProcessedListeners;

  private final ShutdownHandler shutdownHandler;

  private boolean backupTriggerEnabled = true;

  private LocalDateTime eventsLastPushedTime = LocalDateTime.now();

  private ScheduledExecutorService scheduledExecutorService;

  private boolean eventsSuspendedForTesting;

  public UserEventManager(
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlLoadBalancer sourceControlLoadBalancer,
      SourceControlEventProcessor sourceControlEventProcessor,
      SourceControlProvider sourceControlProvider,
      SourceControlUtils sourceControlUtils,
      ShutdownHandler shutdownHandler)
  {
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlLoadBalancer = sourceControlLoadBalancer;
    this.sourceControlEventProcessor = sourceControlEventProcessor;
    eventCostSelectionRule = new EventCostSelectionRule(sourceControlProvider);
    repositoryUrlErrorRule = new RepositoryUrlErrorRule(sourceControlUtils);
    this.simultaneousEventSelectionRule = new SimultaneousEventSelectionRule(sourceControlProvider);
    eventProcessedListeners = ImmutableList.of(applicationScopeEventProcessingSuspensionRule,
        eventProcessingErrorRetryRule, performanceThrottlingRule, repositoryUrlErrorRule);
    this.shutdownHandler = shutdownHandler;
    startBackupEventPushTrigger();
  }

  public void addEvent(SourceControlEvent event) {
    synchronized (prioritizedEventMap) {
      log.debug("New source control event '{}' of type '{}' for application '{}' received", event.getId(),
          event.getEventType(), event.getApplicationId());
      prioritizeEvent(event);
      pushEvents();
    }
  }

  @Override
  public void onEventCompleted(SourceControlEvent event) {
    synchronized (prioritizedEventMap) {
      log.debug("Source control event '{}' of type '{}' for application '{}' complete", event.getId(),
          event.getEventType(), event.getApplicationId());
      sourceControlEventDAO.markEventComplete(event.getId());
      eventsInProgress.remove(event.getApplicationId());
      notifyEventProcessedListeners(event);
      balanceRetryAndPushEvents();
    }
  }

  @Override
  public void onEventPartiallyCompleted(SourceControlEvent event, String reason, Exception e) {
    synchronized (prioritizedEventMap) {
      log.debug("Source control event event '{}' of type '{}' for application '{}' partially complete because {}",
          event.getId(), event.getEventType(), event.getApplicationId(), reason);
      sourceControlEventDAO.markEventPartiallyComplete(event.getId(), reason, e);
      eventsInProgress.remove(event.getApplicationId());
      notifyEventProcessedListeners(event);
      balanceRetryAndPushEvents();
    }
  }

  @Override
  public void onEventError(SourceControlEvent event, Exception e) {
    synchronized (prioritizedEventMap) {
      log.debug("Error processing source control event '{}' of type '{}' for application '{}': {}", event.getId(),
          event.getEventType(), event.getApplicationId(), e.getMessage(), e);
      sourceControlEventDAO.markEventHasError(event.getId(), e.getMessage(), e);
      eventsInProgress.remove(event.getApplicationId());
      handleEventProcessingError(event, e);
    }
  }

  @Override
  public void onEventStarted(SourceControlEvent event) {
    sourceControlEventDAO.markEventInProgress(event.getId());
  }

  public void stop() {
    if (null != scheduledExecutorService) {
      scheduledExecutorService.shutdown();
      scheduledExecutorService = null;
    }
  }

  @VisibleForTesting
  void setBackupTriggerEnabled(boolean enabled) {
    backupTriggerEnabled = enabled;
  }

  private void balanceRetryAndPushEvents() {
    SourceControlEvent anEvent = getAnEvent();
    if (null == anEvent) {
      // we don't have any events queued up to process
      return;
    }

    if (sourceControlLoadBalancer.reserveEvent(anEvent)) {
      processRetryEvents();
      pushEvents();
    }
    else {
      clearPendingEvents();
      sourceControlLoadBalancer.releaseRelatedEvents(anEvent, !areAnyEventsInProgress());
    }
  }

  private void clearPendingEvents() {
    retryEventBucket.clear();
    prioritizedEventMap.clear();
  }

  // return a source control event this user event manager is responsible for
  private SourceControlEvent getAnEvent() {
    SourceControlEvent result = null;

    for (List<SourceControlEvent> prioritizedEvents : prioritizedEventMap.values()) {
      if (!prioritizedEvents.isEmpty()) {
        result = prioritizedEvents.get(0);
        break;
      }
    }

    if (null == result && !retryEventBucket.isEmpty()) {
      result = retryEventBucket.get(0);
    }

    return result;
  }

  private void handleEventProcessingError(SourceControlEvent event, Exception e) {
    applicationScopeEventProcessingSuspensionRule.onEventProcessingError(event, e);
    userScopeEventProcessingSuspensionRule.onEventProcessingError(event, e);
    repositoryUrlErrorRule.onEventProcessingError(event, e);
    if (eventProcessingErrorRetryRule.shouldRetry(event, e)) {
      retryEvent(event);
    }
  }

  private boolean areAnyEventsInProgress() {
    return !eventsInProgress.isEmpty();
  }

  /**
   * push event rules:
   * - only one event in progress per application
   * - event cost plus cost of events already in progress must be <= the max allowed event cost
   * - cannot exceed the allowed in progress count per event type
   * - if the allowed count < 0 that means there is no limit
   */
  private boolean canPushEvent(SourceControlEvent event, int eventPointsAvailable, boolean useStrictEventCounts) {
    return singleApplicationSelectionRule.canPushEvent(event, eventsInProgress)
        && eventCostSelectionRule.canPushEvent(event, eventPointsAvailable)
        && simultaneousEventSelectionRule.canPushEvent(event, eventsInProgress, useStrictEventCounts)
        && applicationScopeEventProcessingSuspensionRule.canPushEvent(event)
        && userScopeEventProcessingSuspensionRule.canPushEvent(event)
        && performanceThrottlingRule.canPushEvents();
  }

  private void notifyEventProcessedListeners(final SourceControlEvent event) {
    eventProcessedListeners.forEach(listener -> listener.onEventProcessed(event));
  }

  private void prioritizeEvent(SourceControlEvent event) {
    List<SourceControlEvent> prioritizedEvents =
        prioritizedEventMap.computeIfAbsent(event.getEventPriority(), k -> new ArrayList<>());
    prioritizedEvents.add(event);
    log.trace("Source control event '{}' of type '{}' for application '{}' prioritized", event.getId(),
        event.getEventType(), event.getApplicationId());
  }

  private void pushEvents() {
    if (eventsSuspendedForTesting) {
      log.trace("Source control events suspended for testing");
      return;
    }
    int eventPointsAvailable =
        eventCostSelectionRule.getAvailableEventPoints(new ArrayList<>(eventsInProgress.values()));
    for (List<SourceControlEvent> prioritizedEvents : prioritizedEventMap.values()) {
      eventPointsAvailable = pushEvents(prioritizedEvents, eventPointsAvailable, true);
      if (eventPointsAvailable <= 0) {
        break;
      }
    }
    // if there are still enough points available to process a remediation PR event, for example, make another
    // pass thru the prioritized events using less strict event selection rules
    if (eventPointsAvailable >= EventCostSelectionRule.REMEDIATION_PR_EVENT_POINTS) {
      for (List<SourceControlEvent> prioritizedEvents : prioritizedEventMap.values()) {
        eventPointsAvailable = pushEvents(prioritizedEvents, eventPointsAvailable, false);
        if (eventPointsAvailable <= 0) {
          break;
        }
      }
    }
    eventsLastPushedTime = LocalDateTime.now();
  }

  private int pushEvents(List<SourceControlEvent> events, int eventPointsAvailable, boolean strictMode) {
    List<SourceControlEvent> pushedEvents = new ArrayList<>();
    List<SourceControlEvent> ignoredEvents = new ArrayList<>();

    for (SourceControlEvent event : events) {
      if (!repositoryUrlErrorRule.canPushEvent(event)) {
        log.trace("Repository URL error limit exceeded for source control event '{}'", event.getEventType());
        sourceControlEventDAO.markEventHasError(event.getId(), "Repository URL error limit exceeded", null);
        ignoredEvents.add(event);
        continue;
      }
      if (canPushEvent(event, eventPointsAvailable, strictMode)) {
        pushEvent(event);
        pushedEvents.add(event);
        eventPointsAvailable -= eventCostSelectionRule.getEventCost(event);
      }
    }

    events.removeAll(pushedEvents);
    events.removeAll(ignoredEvents);

    return eventPointsAvailable;
  }

  private void pushEvent(SourceControlEvent event) {
    try {
      sourceControlEventProcessor.processEvent(event, this);
      eventsInProgress.put(event.getApplicationId(), event);
      log.debug("Sent source control event '{}' of type '{}' for application '{}' for processing", event.getId(),
          event.getEventType(), event.getApplicationId());
    }
    catch (Exception e) {
      log.debug("Unable to process source control event '{}' of type '{}' for application '{}' because {}",
          event.getId(), event.getEventType(), event.getApplicationId(), e.getMessage(), e);
    }
  }

  private void retryEvent(SourceControlEvent event) {
    log.debug("Will retry source control event '{}' of type '{}' for application '{}'", event.getId(),
        event.getEventType(), event.getApplicationId());
    SourceControlEvent retryEvent = event.copyAsNew().setEventStatusDetails("retry");
    sourceControlEventDAO.insert(retryEvent);
    retryEventBucket.add(retryEvent);
  }

  private void processRetryEvents() {
    List<SourceControlEvent> retryNowEvents = new ArrayList<>();
    retryEventBucket.forEach(retryEvent -> {
      if (applicationScopeEventProcessingSuspensionRule.canPushEvent(retryEvent)) {
        retryNowEvents.add(retryEvent);
      }
    });

    retryEventBucket.removeAll(retryNowEvents);
    retryNowEvents.forEach(event -> {
      log.debug("Retrying source control event '{}' for application '{}'", event.getEventType(),
          event.getApplicationId());
      prioritizeEvent(event);
    });
  }

  private void backupEventPushTrigger() {
    if (backupTriggerEnabled && shouldTriggerEventProcessing()) {
      synchronized (prioritizedEventMap) {
        log.trace("timer triggered event processing");
        balanceRetryAndPushEvents();
      }
    }
  }

  /**
   * The natural triggers for event processing (new events, completed events, error events)
   * are not sufficient in all cases to keep events from sitting idle (event suspension due to errors or performance,
   * for example). Therefore, a simple timer will run periodically to ensure that events continue to flow with
   * minimal undesired interruption.
   */
  private void startBackupEventPushTrigger() {
    scheduledExecutorService = newExecutor();
    Runnable sourceControlEventProcessingTask = () -> {
      OneTimeSystemRunnable oneTimeSystemRunnable = new OneTimeSystemRunnable(this::backupEventPushTrigger);
      try {
        oneTimeSystemRunnable.run();
      }
      catch (RuntimeException e) {
        log.warn("Failed to push source control events", e);
      }
    };

    scheduledExecutorService.scheduleAtFixedRate(sourceControlEventProcessingTask, BACKUP_TRIGGER_STARTUP_DELAY_SECONDS,
        BACKUP_TRIGGER_INTERVAL_SECONDS, TimeUnit.SECONDS);
    log.info("Scheduled backup source control event processing to run every {} second(s) starting in {} second(s)",
        BACKUP_TRIGGER_INTERVAL_SECONDS, BACKUP_TRIGGER_STARTUP_DELAY_SECONDS);
  }

  private boolean shouldTriggerEventProcessing() {
    return eventsLastPushedTime.plusSeconds(EVENT_PUSH_MAX_QUIET_PERIOD_SECONDS).isBefore(LocalDateTime.now());
  }

  // Visible for testing
  ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("UserEventManager-%d").setDaemon(true).build();
    TenantScheduledThreadPoolExecutor tenantScheduledThreadPoolExecutor =
        new TenantScheduledThreadPoolExecutor(1, threadFactory);
    shutdownHandler.add(tenantScheduledThreadPoolExecutor);
    return tenantScheduledThreadPoolExecutor;
  }

  @VisibleForTesting
  UserEventManager setEventsSuspendedForTesting(boolean suspended) {
    eventsSuspendedForTesting = suspended;
    return this;
  }

  @VisibleForTesting
  UserEventManager setSuspensionTimeoutForTesting(int timeoutInSeconds) {
    applicationScopeEventProcessingSuspensionRule.setTimeoutsForTesting(timeoutInSeconds);
    userScopeEventProcessingSuspensionRule.setDefaultSuspensionTimeForTesting(timeoutInSeconds);
    return this;
  }
}
