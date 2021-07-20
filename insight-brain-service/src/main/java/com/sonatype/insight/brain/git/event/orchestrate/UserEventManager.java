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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.PerformanceThrottlingRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.selection.EventCostSelectionRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.EventProcessingErrorRetryRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.EventProcessingSuspensionRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.processing.RepositoryUrlErrorRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.selection.SimultaneousEventSelectionRule;
import com.sonatype.insight.brain.git.event.orchestrate.rule.selection.SingleApplicationSelectionRule;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import com.google.common.annotations.VisibleForTesting;
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

  private final SourceControlEventProcessor sourceControlEventProcessor;

  private final SortedMap<Integer, List<SourceControlEvent>> prioritizedEventMap = new TreeMap<>();

  private final Map<String, SourceControlEvent> eventsInProgress = new HashMap<>();

  private final EventCostSelectionRule eventCostSelectionRule = new EventCostSelectionRule();

  private final EventProcessingErrorRetryRule eventProcessingErrorRetryRule = new EventProcessingErrorRetryRule();

  private final EventProcessingSuspensionRule eventProcessingSuspensionRule = new EventProcessingSuspensionRule();

  private final PerformanceThrottlingRule performanceThrottlingRule = new PerformanceThrottlingRule();

  private final RepositoryUrlErrorRule repositoryUrlErrorRule;

  private final SimultaneousEventSelectionRule simultaneousEventSelectionRule = new SimultaneousEventSelectionRule();

  private final SingleApplicationSelectionRule singleApplicationSelectionRule = new SingleApplicationSelectionRule();

  private boolean backupTriggerEnabled = true;

  private LocalDateTime eventsLastPushedTime = LocalDateTime.now();

  private ScheduledExecutorService scheduledExecutorService;

  private boolean eventsSuspendedForTesting;

  public UserEventManager(
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlEventProcessor sourceControlEventProcessor,
      SourceControlUtils sourceControlUtils)
  {
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlEventProcessor = sourceControlEventProcessor;
    repositoryUrlErrorRule = new RepositoryUrlErrorRule(sourceControlUtils);
    startBackupEventPushTrigger();
  }

  public void addEvent(SourceControlEvent event) {
    synchronized (prioritizedEventMap) {
      log.debug("New event '{}' for application {} received", event.getEventType(), event.getApplicationId());
      prioritizeEvent(event);
      pushEvents();
    }
  }

  @Override
  public void onEventCompleted(SourceControlEvent event) {
    synchronized (prioritizedEventMap) {
      log.debug("Event '{}' for application {} complete", event.getEventType(), event.getApplicationId());
      sourceControlEventDAO.markEventComplete(event.getId());
      eventsInProgress.remove(event.getApplicationId());
      repositoryUrlErrorRule.onEventProcessed(event);
      performanceThrottlingRule.onEventProcessed(event);
      pushEvents();
    }
  }

  @Override
  public void onEventPartiallyCompleted(SourceControlEvent event, String reason) {
    synchronized (prioritizedEventMap) {
      log.debug("Event '{}' for application {} partially complete because {}", event.getEventType(),
          event.getApplicationId(), reason);
      sourceControlEventDAO.markEventPartiallyComplete(event.getId(), reason);
      eventsInProgress.remove(event.getApplicationId());
      repositoryUrlErrorRule.onEventProcessed(event);
      performanceThrottlingRule.onEventProcessed(event);
      pushEvents();
    }
  }

  @Override
  public void onEventError(SourceControlEvent event, Exception e) {
    synchronized (prioritizedEventMap) {
      log.debug("Error processing event {} for application {}: {}", event.getEventType(), event.getApplicationId(),
          e.getMessage(), e);
      sourceControlEventDAO.markEventHasError(event.getId(), e.getMessage());
      eventsInProgress.remove(event.getApplicationId());
      // for now, we simply don't push events if the last event completed in error
      // in the future we'll have to figure out what to do with these events, see INT-5378
      handleEventProcessingError(event, e);
    }
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

  private void handleEventProcessingError(SourceControlEvent event, Exception e) {
    eventProcessingSuspensionRule.onEventProcessingError(e);
    repositoryUrlErrorRule.onEventProcessingError(event, e);
    if (eventProcessingErrorRetryRule.shouldRetry(e)) {
      retryEvent(event);
    }
  }

  /**
   * push event rules:
   * - only one event in progress per application
   * - event cost plus cost of events already in progress must be <= the max allowed event cost
   * - cannot exceed the allowed in progress count per event type
   * - if the allowed count < 0 that means there is no limit
   */
  private boolean canPushEvent(SourceControlEvent event, int eventPointsAvailable, boolean useStrictEventCounts) {
    return eventProcessingSuspensionRule.canPushEvent(event)
        && performanceThrottlingRule.canPushEvents()
        && singleApplicationSelectionRule.canPushEvent(event, eventsInProgress)
        && eventCostSelectionRule.canPushEvent(event, eventPointsAvailable)
        && simultaneousEventSelectionRule.canPushEvent(event, eventsInProgress, useStrictEventCounts);
  }

  private void prioritizeEvent(SourceControlEvent event) {
    List<SourceControlEvent> prioritizedEvents =
        prioritizedEventMap.computeIfAbsent(event.getEventPriority(), k -> new ArrayList<>());
    prioritizedEvents.add(event);
    log.trace("Event '{}' for application {} prioritized", event.getEventType(), event.getApplicationId());
  }

  private void pushEvents() {
    if (eventsSuspendedForTesting) {
      log.trace("events suspended for testing");
      return;
    }
    if (eventProcessingSuspensionRule.isEventProcessingSuspended()) {
      log.trace("event processing suspended");
      return;
    }
    int eventPointsAvailable =
        eventCostSelectionRule.getAvailableEventPoints(new ArrayList<>(eventsInProgress.values()));
    for (List<SourceControlEvent> prioritizedEvents : prioritizedEventMap.values()) {
      if (eventProcessingSuspensionRule.isEventProcessingSuspended()) {
        log.trace("event processing suspended");
        return;
      }
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
        log.trace("Repository URL error limit exceeded for '{}'", event.getEventType());
        sourceControlEventDAO.markEventHasError(event.getId(), "Repository URL error limit exceeded");
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
    eventsInProgress.put(event.getApplicationId(), event);
    sourceControlEventDAO.markEventInProgress(event.getId());

    sourceControlEventProcessor.processEvent(event, this);
    log.debug("Sent application {} event {} for processing", event.getApplicationId(), event.getEventType());
  }

  private void retryEvent(SourceControlEvent event) {
    log.trace("todo {}", event.getApplicationId());
  }

  /**
   * The natural triggers for event processing (new events, completed events, loading events from DB on startup)
   * are not sufficient in all cases to keep events from sitting idle (event suspension due to errors or performance,
   * for example).  Therefore, a simple timer will run periodically to ensure that events continue to flow with
   * minimal undesired interruption.
   */
  private void startBackupEventPushTrigger() {
    scheduledExecutorService = newExecutor();
    Runnable sourceControlEventProcessingTask = new SystemRunnable(() -> {
      try {
        if (backupTriggerEnabled && shouldTriggerEventProcessing()) {
          synchronized (prioritizedEventMap) {
            log.trace("timer triggered event processing");
            pushEvents();
          }
        }
      }
      catch (RuntimeException e) {
        log.warn("Failed to push source control events", e);
      }
    });
    scheduledExecutorService.scheduleAtFixedRate(sourceControlEventProcessingTask, BACKUP_TRIGGER_STARTUP_DELAY_SECONDS,
        BACKUP_TRIGGER_INTERVAL_SECONDS, TimeUnit.SECONDS);
    log.info("Scheduled source control event heartbeat to run every {} second(s) starting in {} second(s)",
        BACKUP_TRIGGER_INTERVAL_SECONDS, BACKUP_TRIGGER_STARTUP_DELAY_SECONDS);
  }

  private boolean shouldTriggerEventProcessing() {
    return eventsLastPushedTime.plusSeconds(EVENT_PUSH_MAX_QUIET_PERIOD_SECONDS).isBefore(LocalDateTime.now());
  }

  private ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("UserEventManager-%d").setDaemon(true).build();
    return new ScheduledThreadPoolExecutor(1, threadFactory);
  }

  @VisibleForTesting
  UserEventManager setEventsSuspendedForTesting(boolean suspended) {
    eventsSuspendedForTesting = suspended;
    return this;
  }
}
