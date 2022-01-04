/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.concurrent.SemaphorePool;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.GitCommitStatusService;
import com.sonatype.insight.brain.git.PullRequestCommentingEventHandler;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.SourceControlException;
import com.sonatype.insight.brain.git.SourceControlInstanceManager;
import com.sonatype.insight.brain.git.SourceControlScanService;
import com.sonatype.insight.brain.git.SourceControlService;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.CurrentUser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

/**
 * @deprecated in favor of the #SourceControlEventOrchestrator in IQ 129.  The intent is to delete this class once it's
 * verified in the field there are no significant issues with the new orchestrator.
 *
 * This class handles the interactions with the durable event queue (i.e. DB table) for source control events.
 * These events represent things like responses to discovered pull requests and responses to policy evaluations
 * that may or may not have a corresponding pull request associated with them.
 *
 * This class is responsible for publishing events to the durable queue and for retrieving events from that queue
 * and submitting them for processing.  As such, this class handles the state transitions for the events, which are:
 * NEW : represents a newly submitted event
 * IN PROGRESS : the event is currently being processed
 * COMPLETE : the event was successfully processed
 * ERROR : there was an error processing the event
 *
 * This class is also multi-node compatible in that it can run from multiple instances of IQ server.  This class
 * 'reserves' events for itself prior to pulling and working them, thus preventing the same event from being processed
 * by multiple instances.  However, this is not good enough as events targeting the same SCM user must be handled
 * by the same instance so that simultaneous SCM API requests are not submitted for the same user.  The problem of
 * assigning events to particular IQ instances will be solved in the future, prior to releasing official support for
 * multi-node IQ.
 */
@Deprecated
@Named
@Singleton
public class SourceControlEventService
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventService.class);

  @VisibleForTesting
  static final int THREAD_POOL_SIZE = 15;

  // this gives us the potential to process up to 4800 events per hour (polled 4 times/minute), but
  // load testing has revealed that we can only process about 1/3 of that in that time on the test machine;  so,
  // this gives us some ability to scale to multiple configured SCM users and/or better hardware
  @VisibleForTesting
  static final int TASK_QUEUE_CAPACITY = 20;

  @VisibleForTesting
  static final int MAX_LOAD = THREAD_POOL_SIZE + TASK_QUEUE_CAPACITY;

  // arbitrarily picking 2 minutes to detect when another instance of IQ server has gone down/offline and is no longer
  // processing events
  private static final int STALE_EVENT_CUTOFF_MS = 1_000 * 120;

  private final AtomicBoolean inProcessing = new AtomicBoolean();

  /*
    work for the same repo/application should be done sequentially; work for different apps can be done in parallel
   */
  private SemaphorePool repoAccessController = new SemaphorePool(THREAD_POOL_SIZE);

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlInstanceManager sourceControlInstanceManager;

  private final PullRequestCommentingEventHandler pullRequestCommentingEventHandler;

  private ThreadPoolExecutor threadPoolExecutor;

  private final PullRequestRemediationService pullRequestRemediationService;

  private final GitCommitStatusService gitCommitStatusService;

  private final SourceControlScanService sourceControlScanService;

  private final SourceControlService sourceControlService;

  private final CurrentUser currentUser;

  @Inject
  public SourceControlEventService(
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlInstanceManager sourceControlInstanceManager,
      PullRequestCommentingEventHandler pullRequestCommentingEventHandler,
      PullRequestRemediationService pullRequestRemediationService,
      GitCommitStatusService gitCommitStatusService,
      SourceControlScanService sourceControlScanService,
      SourceControlService sourceControlService,
      CurrentUser currentUser)
  {
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlInstanceManager = sourceControlInstanceManager;
    this.pullRequestCommentingEventHandler = pullRequestCommentingEventHandler;
    this.pullRequestRemediationService = pullRequestRemediationService;
    this.gitCommitStatusService = gitCommitStatusService;
    this.sourceControlScanService = sourceControlScanService;
    this.sourceControlService = sourceControlService;
    this.currentUser = currentUser;
  }

  /**
   * Initiates a cycle to pull events from the event queue (DB table) and submit them to the internal thread pool
   * for execution.
   *
   * @return the count of events that were submitted for execution
   */
  public int processEvents() {
    // for now this is a global check;  future plan is to base this on specific tokens/repos/users
    if (!sourceControlInstanceManager.canProcessEvents()) {
      log.trace("This instance is not allowed to process events.");
      return 0;
    }

    int eventsSubmittedForProcessing = 0;

    if (inProcessing.get()) {
      log.debug("skipping event processing this cycle as previous cycle is still running");
    }
    else {
      try {
        inProcessing.set(true);

        int numberOfEventsToRequest = getNumberOfEventsToRequest();

        if (numberOfEventsToRequest > 0) {
          // un-claim any events where it appears that the instance processing them is no longer working
          sourceControlEventDAO
              .resetStaleEvents(new Date(currentTimeMillis() - STALE_EVENT_CUTOFF_MS), getInstanceId());

          sourceControlEventDAO.reserveEventsForInstance(getInstanceId());

          List<SourceControlEvent> events =
              sourceControlEventDAO.selectEventsForInstance(getInstanceId(), numberOfEventsToRequest);

          log.debug("Requested {} source control events, processing {}", numberOfEventsToRequest, events.size());

          for (SourceControlEvent event : events) {
            if (!hasCapacity()) {
              break;
            }
            if (markEventInProgress(event)) {
              getThreadPoolExecutor().execute(() -> handleSourceControlEvent(event));
              eventsSubmittedForProcessing++;
              try {
                // give thread pool time to startup event handling as we'd like the events to be processed in the order
                // received as much as possible
                sleep(10);
              }
              catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
          }
        }
      }
      finally {
        inProcessing.set(false);
      }
    }

    return eventsSubmittedForProcessing;
  }

  private boolean markEventInProgress(SourceControlEvent event) {
    try {
      sourceControlEventDAO.markEventInProgress(event.getId());
      return true;
    }
    catch (Exception e) {
      log.error("Error marking event in progress for event '{}' of type '{}' for application '{}' : {}",
          event.getId(), event.getEventType(), event.getApplicationId(), e.getMessage(), e);
      return false;
    }
  }

  private void checkRunsAsSystem(SourceControlEvent event) {
    // See https://issues.sonatype.org/browse/INT-5413
    String username = currentUser.getUsernameOrSystem();
    if (!CurrentUser.SYSTEM.equals(username)) {
      throw new IllegalStateException("SourceControlEvent with ID " + event.getId() + " processed as user '" + username
          + "' instead of '" + CurrentUser.SYSTEM + "'");
    }
  }

  private void handleSourceControlEvent(final SourceControlEvent event) {
    try {
      log.trace("Handling event '{}' of type '{}' for application '{}'", event.getId(), event.getEventType(),
          event.getApplicationId());

      checkRunsAsSystem(event);

      if (acquireRepoAccess(event.getApplicationId())) {
        log.trace("Acquired repo access for event '{}' of type '{}' for application '{}'", event.getId(),
            event.getEventType(), event.getApplicationId());
        try {
          if (executeSourceControlEvent(event)) {
            log.debug("Processed event '{}' of type '{}' for application '{}'", event.getId(), event.getEventType(),
                event.getApplicationId());
            sourceControlEventDAO.markEventComplete(event.getId());
          }
        }
        catch (Exception e) {
          log.error("Error updating event processing status for event '{}' of type '{}' for application '{}' : {}",
              event.getId(), event.getEventType(), event.getApplicationId(), e.getMessage(), e);
        }
        finally {
          releaseRepoAccess(event.getApplicationId());
          log.trace("Released repo access for event '{}' of type '{}' for application '{}'", event.getId(),
              event.getEventType(), event.getApplicationId());
        }
      }
      notifyFinishedProcessingEvent(event);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  @VisibleForTesting
  void notifyFinishedProcessingEvent(@SuppressWarnings("unused") SourceControlEvent event) {
    // tests will 'spy' on this method to know when the processing of this event is finished and the test can start
    // its validations (since this work occurs in a separate thread)
  }

  private boolean acquireRepoAccess(String applicationId) {
    try {
      repoAccessController.acquire(applicationId);
      return true;
    }
    catch (InterruptedException e) {
      log.debug("Unable to acquire repo access for application '{}'", applicationId, e);
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private void releaseRepoAccess(String applicationId) {
    try {
      repoAccessController.release(applicationId);
    }
    catch (InterruptedException e) {
      log.error("Unable to release repo access for application '{}'", applicationId, e);
      Thread.currentThread().interrupt();
    }
  }

  private boolean executeSourceControlEvent(SourceControlEvent event) {
    boolean success = true;

    try {
      switch (event.getEventType()) {
        case SourceControlEvent.APPLICATION_EVALUATION_EVENT:
          pullRequestCommentingEventHandler.onApplicationEvaluation(event);
          break;

        case SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT:
          pullRequestCommentingEventHandler.onDiscoveredPullRequest(event);
          break;

        case SourceControlEvent.UPDATED_PULL_REQUEST_EVENT:
          pullRequestCommentingEventHandler.onUpdatedPullRequest(event);
          break;

        case SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT:
          sourceControlScanService.onSourceControlScan(event);
          break;

        case SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT:
          pullRequestRemediationService.onRemediateComponent(event);
          break;

        case SourceControlEvent.STATUS_UPDATE_EVENT:
          gitCommitStatusService.onSendCommitStatus(event);
          break;

        case SourceControlEvent.REPOSITORY_URL_UPDATED_EVENT:
          sourceControlService.onRepositoryUrlUpdated(event);
          break;

        default:
          log.warn("Invalid source control event type '{}' for event '{}'", event.getEventType(), event.getId());
          success = false;
          sourceControlEventDAO.markEventHasError(event.getId(), "invalid event type", null);
      }
    }
    catch (Exception e) {
      success = false;
      handleException(event, e);
    }

    return success;
  }

  private void handleException(SourceControlEvent event, Exception e) {
    if (e instanceof SourceControlException && ((SourceControlException)e).isPartialFailure()) {
      log.warn("Partially processed event '{}' of type '{}' for application '{}' : {}", event.getId(),
          event.getEventType(), event.getApplicationId(), e.getMessage(), e);
      sourceControlEventDAO.markEventPartiallyComplete(event.getId(), e.getMessage(), e);
    }
    else {
      log.error("Unable to process event '{}' of type '{}' for application '{}' : {}", event.getId(),
          event.getEventType(), event.getApplicationId(), e.getMessage(), e);
      sourceControlEventDAO.markEventHasError(event.getId(), e.getMessage(), e);
    }
  }

  @VisibleForTesting
  String getInstanceId() {
    return sourceControlInstanceManager.getSourceControlInstanceId();
  }

  private void initThreadPoolExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("SourceControlEventService-%s")
        .build();
    threadPoolExecutor = new ThreadPoolExecutor(
        THREAD_POOL_SIZE,
        THREAD_POOL_SIZE,
        30L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(TASK_QUEUE_CAPACITY),
        threadFactory);
    threadPoolExecutor.allowCoreThreadTimeOut(true);
  }

  private ThreadPoolExecutor getThreadPoolExecutor() {
    if (null == threadPoolExecutor) {
      initThreadPoolExecutor();
    }
    return threadPoolExecutor;
  }

  int getNumberOfEventsToRequest() {
    return getRemainingCapacity();
  }

  private int getRemainingCapacity() {
    return getThreadPoolExecutor().getQueue().remainingCapacity();
  }

  private boolean hasCapacity() {
    return getRemainingCapacity() > 0;
  }

  @VisibleForTesting
  void setRepoAccessController(SemaphorePool repoAccessController) {
    this.repoAccessController = repoAccessController;
  }

  @VisibleForTesting
  void shutdown() {
    try {
      if (null != threadPoolExecutor) {
        threadPoolExecutor.shutdownNow();
        while (!threadPoolExecutor.isShutdown()) {
          try {
            sleep(100);
          }
          catch (InterruptedException e) {
            currentThread().interrupt();
            break;
          }
        }
      }
    }
    finally {
      notifyShutdownComplete();
    }
  }

  @VisibleForTesting
  void notifyShutdownComplete() {
    // tests will 'spy' on this method to know when the the threads have been shutdown and this service has no more
    // work pending
  }
}
