/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.concurrent.LazyInitThreadPoolExecutor;
import com.sonatype.insight.brain.concurrent.SemaphorePool;
import com.sonatype.insight.brain.git.GitCommitStatusService;
import com.sonatype.insight.brain.git.PullRequestCommentingEventHandler;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.PullRequestStateEventHandler;
import com.sonatype.insight.brain.git.SourceControlException;
import com.sonatype.insight.brain.git.SourceControlScanService;
import com.sonatype.insight.brain.git.SourceControlService;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.common.metering.TaggedRunnable;
import com.sonatype.insight.brain.tenancy.TenantReference;
import io.micrometer.core.instrument.Tags;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;

@Named
@Singleton
public class SourceControlEventProcessor
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlEventProcessor.class);

  /**
   * The thread pool size can be modified with by changing the sourceControlEventProcessorMaxThreadPoolSize system
   * configuration property however the IQ instance must be restarted for this change to take effect
   */
  public static final int DEFAULT_MAX_THREAD_POOL_SIZE = 50;

  // want to keep the threads alive a little longer than the PR polling interval so they are available for reuse
  private static final long CORE_THREAD_KEEP_ALIVE_SECONDS = 75L;

  @VisibleForTesting
  static final String REPO_ACCESS_LOCK_ERROR = "Unable to process event. Could not acquire the repo access lock.";

  /*
   * work for the same repo/application should be done sequentially; work for different apps can be done in parallel.
   *
   * Note (12/2/2021): We control this via the IQ application ID. There could be multiple IQ applications for the
   * same repo, which means we would have multiple git workspaces for the same repo (different, app specific
   * folders, though, so no real problem here - just something to keep in mind).
   */
  private TenantReference<SemaphorePool> repoAccessController;

  private LazyInitThreadPoolExecutor lazyInitThreadPoolExecutor;

  private final PullRequestCommentingEventHandler pullRequestCommentingEventHandler;

  private final PullRequestStateEventHandler pullRequestStateEventHandler;

  private final PullRequestRemediationService pullRequestRemediationService;

  private final GitCommitStatusService gitCommitStatusService;

  private final SourceControlScanService sourceControlScanService;

  private final SourceControlService sourceControlService;

  private final CurrentUser currentUser;

  @Inject
  public SourceControlEventProcessor(
      PullRequestCommentingEventHandler pullRequestCommentingEventHandler,
      PullRequestStateEventHandler pullRequestStateEventHandler,
      PullRequestRemediationService pullRequestRemediationService,
      GitCommitStatusService gitCommitStatusService,
      SourceControlScanService sourceControlScanService,
      SourceControlService sourceControlService,
      CurrentUser currentUser,
      Configuration configuration,
      ShutdownHandler shutdownHandler)
  {
    this.pullRequestCommentingEventHandler = pullRequestCommentingEventHandler;
    this.pullRequestStateEventHandler = pullRequestStateEventHandler;
    this.pullRequestRemediationService = pullRequestRemediationService;
    this.gitCommitStatusService = gitCommitStatusService;
    this.sourceControlScanService = sourceControlScanService;
    this.sourceControlService = sourceControlService;
    this.currentUser = currentUser;

    int threadPoolSize = configuration.getSourceControlEventProcessorPoolSize();

    repoAccessController = new TenantReference<>(() -> new SemaphorePool(threadPoolSize));
    lazyInitThreadPoolExecutor = new LazyInitThreadPoolExecutor(threadPoolSize, threadPoolSize,
        "SourceControlEventProcessor-%s", CORE_THREAD_KEEP_ALIVE_SECONDS, shutdownHandler)
            .setShouldClearShiroThreadContextBeforeThreadStart(true);
  }

  // Visible for testing
  LazyInitThreadPoolExecutor getLazyInitThreadPoolExecutor() {
    return lazyInitThreadPoolExecutor;
  }

  public void processEvent(SourceControlEvent event, SourceControlEventStatusListener statusListener) {
    lazyInitThreadPoolExecutor.getThreadPoolExecutor()
        .execute(new TaggedRunnable(
            () -> handleSourceControlEventAndExitOnError(event, statusListener),
            Tags.of("source_control_event_type", event.getEventType().replaceAll(" ", "_"))));
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

      managedEvent.onEventStarted();

      if (!acquireResourceLock(managedEvent.getSourceControlEvent())) {
        throw new RuntimeException(REPO_ACCESS_LOCK_ERROR);
      }
      try {
        String eventType = managedEvent.getEventType();
        if (SourceControlEvent.STATUS_UPDATE_EVENT.equals(eventType)) {
          log.trace("Processing STATUS_UPDATE_EVENT '{}' without locking", managedEvent.getId());
        }
        else {
          log.trace("Acquired repo access for event '{}' of type '{}' for application '{}'",
              managedEvent.getId(), eventType, managedEvent.getApplicationId());
        }

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
        releaseResourceLock(managedEvent.getSourceControlEvent());
      }
    }
    finally {
      notifyFinishedProcessingEvent(managedEvent.getSourceControlEvent());
    }
  }

  @VisibleForTesting
  void notifyFinishedProcessingEvent(@SuppressWarnings("unused") SourceControlEvent event) {
    // tests will 'spy' on this method to know when the processing of this event is finished and the test can start
    // its validations (since this work occurs in a separate thread)
  }

  private boolean acquireResourceLock(SourceControlEvent event) {
    if (SourceControlEvent.STATUS_UPDATE_EVENT.equals(event.getEventType())) {
      log.trace("No lock required for STATUS_UPDATE_EVENT '{}'", event.getId());
      return true;
    }

    // All other events use existing application-level locking for safety
    return acquireRepoAccess(event.getApplicationId());
  }

  private void releaseResourceLock(SourceControlEvent event) {
    if (SourceControlEvent.STATUS_UPDATE_EVENT.equals(event.getEventType())) {
      log.trace("No lock to release for STATUS_UPDATE_EVENT '{}'", event.getId());
      return;
    }

    log.trace("Released repo access for event '{}' of type '{}' for application '{}'",
        event.getId(), event.getEventType(), event.getApplicationId());
    releaseRepoAccess(event.getApplicationId());
  }

  private boolean acquireRepoAccess(String applicationId) {
    try {
      repoAccessController.get().acquire(applicationId);
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
      repoAccessController.get().release(applicationId);
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

        case SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT, SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT:
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

        case SourceControlEvent.PR_STATE_UPDATE_EVENT:
        case SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT:
          pullRequestStateEventHandler.handle(event);
          break;

        case SourceControlEvent.CLOSE_PULL_REQUEST_EVENT:
          pullRequestRemediationService.onRemediatePullRequestClosing(event);
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
      event.onPartiallyComplete(e.getMessage(), e);
    }
    else {
      log.error("Unable to process event '{}' of type '{}' for application '{}' : {}", event.getId(),
          event.getEventType(), event.getApplicationId(), e.getMessage(), e);
      event.onError(e);
    }
  }

  @VisibleForTesting
  void setRepoAccessController(TenantReference<SemaphorePool> repoAccessController) {
    this.repoAccessController = repoAccessController;
  }

  @Override
  public void stop() {
    try {
      lazyInitThreadPoolExecutor.shutdown();
    }
    finally {
      notifyShutdownComplete();
    }
  }

  @VisibleForTesting
  void notifyShutdownComplete() {
    // tests will 'spy' on this method to know when the threads have been shutdown and this service has no more
    // work pending
  }

  /**
   * The purpose of the ManagedSourceControlEvent is to capture, retain, and interact with the 'listener' that
   * will receive the final status/outcome of the processed event.
   */
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

    public void onPartiallyComplete(String reason, Exception e) {
      sourceControlEventStatusListener.onEventPartiallyCompleted(sourceControlEvent, reason, e);
    }

    public void onEventStarted() {
      sourceControlEventStatusListener.onEventStarted(sourceControlEvent);
    }
  }
}
