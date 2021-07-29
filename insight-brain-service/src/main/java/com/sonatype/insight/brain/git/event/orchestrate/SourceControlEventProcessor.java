/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.concurrent.LazyInitThreadPoolExecutor;
import com.sonatype.insight.brain.concurrent.SemaphorePool;
import com.sonatype.insight.brain.git.GitCommitStatusService;
import com.sonatype.insight.brain.git.PullRequestCommentingEventHandler;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.SourceControlException;
import com.sonatype.insight.brain.git.SourceControlScanService;
import com.sonatype.insight.brain.git.SourceControlService;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.CurrentUser;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SourceControlEventProcessor
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventProcessor.class);

  @VisibleForTesting
  static final int THREAD_POOL_SIZE = 25;

  @VisibleForTesting
  static final int TASK_QUEUE_CAPACITY = THREAD_POOL_SIZE;

  // want to keep the threads alive a little longer than the PR polling interval so they are available for reuse
  private static final long CORE_THREAD_KEEP_ALIVE_SECONDS = 75L;

  @VisibleForTesting
  static final String REPO_ACCESS_LOCK_ERROR = "Unable to process event.  Could not acquire the repo access lock.";

  /*
    work for the same repo/application should be done sequentially; work for different apps can be done in parallel
   */
  private SemaphorePool repoAccessController = new SemaphorePool(THREAD_POOL_SIZE);

  private final PullRequestCommentingEventHandler pullRequestCommentingEventHandler;

  private final PullRequestRemediationService pullRequestRemediationService;

  private final GitCommitStatusService gitCommitStatusService;

  private final SourceControlScanService sourceControlScanService;

  private final SourceControlService sourceControlService;

  private final CurrentUser currentUser;

  private final LazyInitThreadPoolExecutor lazyInitThreadPoolExecutor =
      new LazyInitThreadPoolExecutor(THREAD_POOL_SIZE, TASK_QUEUE_CAPACITY, "SourceControlEventProcessor-%s",
          CORE_THREAD_KEEP_ALIVE_SECONDS)
          .setShouldClearShiroThreadContextBeforeThreadStart(true);

  @Inject
  public SourceControlEventProcessor(
      PullRequestCommentingEventHandler pullRequestCommentingEventHandler,
      PullRequestRemediationService pullRequestRemediationService,
      GitCommitStatusService gitCommitStatusService,
      SourceControlScanService sourceControlScanService,
      SourceControlService sourceControlService,
      CurrentUser currentUser)
  {
    this.pullRequestCommentingEventHandler = pullRequestCommentingEventHandler;
    this.pullRequestRemediationService = pullRequestRemediationService;
    this.gitCommitStatusService = gitCommitStatusService;
    this.sourceControlScanService = sourceControlScanService;
    this.sourceControlService = sourceControlService;
    this.currentUser = currentUser;
  }

  public void processEvent(SourceControlEvent event, SourceControlEventStatusListener statusListener) {
    lazyInitThreadPoolExecutor.getThreadPoolExecutor()
        .execute(() -> handleSourceControlEventAndExitOnError(event, statusListener));
  }

  private void handleSourceControlEventAndExitOnError(
      SourceControlEvent event,
      SourceControlEventStatusListener statusListener)
  {
    try {
      handleSourceControlEvent(new ManagedSourceControlEvent(event, statusListener));
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

  private void checkRunsAsSystem(String eventId) {
    // See https://issues.sonatype.org/browse/INT-5413
    String username = currentUser.getUsernameOrSystem();
    if (!CurrentUser.SYSTEM.equals(username)) {
      throw new IllegalStateException("SourceControlEvent with ID " + eventId + " processed as user '" + username
          + "' instead of '" + CurrentUser.SYSTEM + "'");
    }
  }

  private void handleSourceControlEvent(final ManagedSourceControlEvent managedEvent) {
    log.trace("Handling event '{}' of type '{}' for application '{}'", managedEvent.getId(),
        managedEvent.getEventType(), managedEvent.getApplicationId());

    try {
      try {
        checkRunsAsSystem(managedEvent.getId());
      }
      catch (Exception e) {
        log.error("Unable to process event '{}' of type '{}' for application '{}' : {}", managedEvent.getId(),
            managedEvent.getEventType(), managedEvent.getApplicationId(), e.getMessage(), e);
        managedEvent.onError(e);
        return;
      }

      if (!acquireRepoAccess(managedEvent.getApplicationId())) {
        throw new RuntimeException(REPO_ACCESS_LOCK_ERROR);
      }
      try {
        log.trace("Acquired repo access for event '{}' of type '{}' for application '{}'", managedEvent.getId(),
            managedEvent.getEventType(), managedEvent.getApplicationId());

        if (executeSourceControlEvent(managedEvent)) {
          log.debug("Processed event '{}' of type '{}' for application '{}'", managedEvent.getId(),
              managedEvent.getEventType(), managedEvent.getApplicationId());
          managedEvent.onComplete();
        }
      }
      catch (Exception e) {
        log.error("Error updating event processing status for event '{}' of type '{}' for application '{}' : {}",
            managedEvent.getId(), managedEvent.getEventType(), managedEvent.getApplicationId(), e.getMessage(), e);
      }
      finally {
        releaseRepoAccess(managedEvent.getApplicationId());
        log.trace("Released repo access for event '{}' of type '{}' for application '{}'", managedEvent.getId(),
            managedEvent.getEventType(), managedEvent.getApplicationId());
      }
    }
    finally {
      notifyFinishedProcessingEvent(managedEvent.getSourceControlEvent());
    }
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
      log.warn("Unable to release repo access for application '{}'", applicationId, e);
      Thread.currentThread().interrupt();
    }
  }

  private boolean executeSourceControlEvent(ManagedSourceControlEvent managedEvent) {
    boolean success = true;
    SourceControlEvent event = managedEvent.getSourceControlEvent();

    try {
      switch (event.getEventType()) {
        case SourceControlEvent.APPLICATION_EVALUATION_EVENT:
          pullRequestCommentingEventHandler.onApplicationEvaluation(event);
          break;

        case SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT:
          pullRequestCommentingEventHandler.onDiscoveredPullRequest(event);
          break;

        case SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT:
          pullRequestRemediationService.onRemediateComponent(event);
          break;

        case SourceControlEvent.REPOSITORY_URL_UPDATED_EVENT:
          sourceControlService.onRepositoryUrlUpdated(event);
          break;

        case SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT:
          sourceControlScanService.onSourceControlScan(event);
          break;

        case SourceControlEvent.STATUS_UPDATE_EVENT:
          gitCommitStatusService.onSendCommitStatus(event);
          break;

        case SourceControlEvent.UPDATED_PULL_REQUEST_EVENT:
          pullRequestCommentingEventHandler.onUpdatedPullRequest(event);
          break;

        default:
          log.warn("Invalid source control event type '{}' for event '{}'", event.getEventType(), event.getId());
          success = false;
          managedEvent.onError(new Exception("invalid event type"));
      }
    }
    catch (Exception e) {
      success = false;
      handleException(managedEvent, e);
    }

    return success;
  }

  private void handleException(ManagedSourceControlEvent event, Exception e) {
    if (e instanceof SourceControlException && ((SourceControlException) e).isPartialFailure()) {
      log.warn("Partially processed event '{}' of type '{}' for application '{}' : {}", event.getId(),
          event.getEventType(), event.getApplicationId(), e.getMessage(), e);
      event.onPartiallyComplete(e.getMessage());
    }
    else {
      log.error("Unable to process event '{}' of type '{}' for application '{}' : {}", event.getId(),
          event.getEventType(), event.getApplicationId(), e.getMessage(), e);
      event.onError(e);
    }
  }

  @VisibleForTesting
  void setRepoAccessController(SemaphorePool repoAccessController) {
    this.repoAccessController = repoAccessController;
  }

  void shutdown() {
    try {
      lazyInitThreadPoolExecutor.shutdown();
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

  public static class ManagedSourceControlEvent
  {
    private final SourceControlEvent sourceControlEvent;

    private final SourceControlEventStatusListener sourceControlEventStatusListener;

    public ManagedSourceControlEvent(
        SourceControlEvent sourceControlEvent,
        SourceControlEventStatusListener sourceControlEventStatusListener)
    {
      this.sourceControlEvent = sourceControlEvent;
      this.sourceControlEventStatusListener = sourceControlEventStatusListener;
    }

    public SourceControlEvent getSourceControlEvent() {
      return sourceControlEvent;
    }

    public String getId() {
      return sourceControlEvent.getId();
    }

    public String getApplicationId() {
      return sourceControlEvent.getApplicationId();
    }

    public String getEventType() {
      return sourceControlEvent.getEventType();
    }

    public void onComplete() {
      sourceControlEventStatusListener.onEventCompleted(sourceControlEvent);
    }

    public void onError(Exception e) {
      sourceControlEventStatusListener.onEventError(sourceControlEvent, e);
    }

    public void onPartiallyComplete(String reason) {
      sourceControlEventStatusListener.onEventPartiallyCompleted(sourceControlEvent, reason);
    }
  }
}
