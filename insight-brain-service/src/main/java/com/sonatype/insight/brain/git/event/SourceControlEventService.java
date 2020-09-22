/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import java.util.Date;
import java.util.List;
import java.util.UUID;
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
import com.sonatype.insight.brain.git.ManifestScanService;
import com.sonatype.insight.brain.git.PullRequestCommentingService;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

/**
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
@Named
@Singleton
public class SourceControlEventService
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventService.class);

  private static final int THREAD_POOL_SIZE = 10;

  @VisibleForTesting
  static final int TASK_QUEUE_CAPACITY = 20;

  @VisibleForTesting
  static final int MAX_LOAD = THREAD_POOL_SIZE + TASK_QUEUE_CAPACITY;

  @VisibleForTesting
  static final String INSTANCE_ID = UUID.randomUUID().toString();

  // arbitrarily picking 2 minutes to detect when another instance of IQ server has gone down/offline and is no longer
  // processing events
  private static final int STALE_EVENT_CUTOFF_MS = 1_000 * 120;

  private final AtomicBoolean inProcessing = new AtomicBoolean();

  /*
    work for the same repo/application should be done sequentially; work for different apps can be done in parallel
   */
  private SemaphorePool repoAccessController = new SemaphorePool(THREAD_POOL_SIZE);

  private final SourceControlEventDAO sourceControlEventDAO;

  private final PullRequestCommentingService pullRequestCommentingService;

  private ThreadPoolExecutor threadPoolExecutor;

  private final PullRequestRemediationService pullRequestRemediationService;

  private final GitCommitStatusService gitCommitStatusService;

  private final ManifestScanService manifestScanService;

  @Inject
  public SourceControlEventService(
      SourceControlEventDAO sourceControlEventDAO,
      PullRequestCommentingService pullRequestCommentingService,
      PullRequestRemediationService pullRequestRemediationService,
      GitCommitStatusService gitCommitStatusService,
      ManifestScanService manifestScanService)
  {
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.pullRequestCommentingService = pullRequestCommentingService;
    this.pullRequestRemediationService = pullRequestRemediationService;
    this.gitCommitStatusService = gitCommitStatusService;
    this.manifestScanService = manifestScanService;
  }

  /**
   * Initiates a cycle to pull events from the event queue (DB table) and submit them to the internal thread pool
   * for execution.
   *
   * @return the count of events that were submitted for execution
   */
  public int processEvents() {
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
          sourceControlEventDAO.resetStaleEvents(new Date(currentTimeMillis() - STALE_EVENT_CUTOFF_MS), INSTANCE_ID);

          sourceControlEventDAO.reserveEventsForInstance(INSTANCE_ID);

          List<SourceControlEvent> events =
              sourceControlEventDAO.selectEventsForInstance(INSTANCE_ID, numberOfEventsToRequest);

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

  private void handleSourceControlEvent(final SourceControlEvent event) {
    log.trace("Handling event '{}' of type '{}' for application '{}'", event.getId(), event.getEventType(),
        event.getApplicationId());

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

  @VisibleForTesting
  void notifyFinishedProcessingEvent(SourceControlEvent event) {
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
          pullRequestCommentingService.onApplicationEvaluation(event);
          break;

        case SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT:
          pullRequestCommentingService.onDiscoveredPullRequest(event);
          break;

        case SourceControlEvent.MANIFEST_SCAN_EVENT:
          manifestScanService.onManifestScan(event);
          break;

        case SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT:
          pullRequestRemediationService.onRemediateComponent(event);
          break;

        case SourceControlEvent.STATUS_UPDATE_EVENT:
          gitCommitStatusService.onSendCommitStatus(event);
          break;

        default:
          log.warn("Invalid source control event type '{}' for event '{}'", event.getEventType(), event.getId());
          success = false;
          sourceControlEventDAO.markEventHasError(event.getId(), "invalid event type");
      }
    }
    catch (Exception e) {
      success = false;
      log.error("Unable to process event '{}' of type '{}' for application '{}' : {}", event.getId(),
          event.getEventType(), event.getApplicationId(), e.getMessage(), e);
      sourceControlEventDAO.markEventHasError(event.getId(), e.getMessage());
    }

    return success;
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
