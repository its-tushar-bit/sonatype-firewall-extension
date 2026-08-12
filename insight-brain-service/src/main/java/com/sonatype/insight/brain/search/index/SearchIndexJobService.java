/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexHealthDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexJobDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexJobEventDAO;
import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJob;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJobEvent;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.utils.ExceptionUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Durable job control for full rebuild / cleanup. Cancel keeps Lucene blue (E0).
 */
@Named
@Singleton
public class SearchIndexJobService
{
  private static final Logger log = LoggerFactory.getLogger(SearchIndexJobService.class);

  /**
   * Maintenance types the model defines and Analyze can recommend, but which have no worker yet.
   * Accepting one would mark it RUNNING with nothing to finish it, leaving an active job that makes
   * every later start conflict. The worker arrives with CLM-44498.
   */
  private static final Set<String> UNIMPLEMENTED_JOB_TYPES = Set.of(
      SearchIndexJob.TYPE_SCOPED_CLEANUP,
      SearchIndexJob.TYPE_POINT_REPAIR,
      SearchIndexJob.TYPE_ATTRIBUTE_BACKFILL);

  private final SearchIndexJobDAO jobDAO;

  private final SearchIndexJobEventDAO jobEventDAO;

  private final SearchIndexHealthDAO healthDAO;

  private final Provider<IndexService> indexService;

  private final Provider<SearchIndexHealthService> healthService;

  private final CurrentUser currentUser;

  /** Serializes start/cancel so findActiveJob + insert cannot race on a single node. */
  private final Object jobControlLock = new Object();

  @Inject
  public SearchIndexJobService(
      final SearchIndexJobDAO jobDAO,
      final SearchIndexJobEventDAO jobEventDAO,
      final SearchIndexHealthDAO healthDAO,
      final Provider<IndexService> indexService,
      final Provider<SearchIndexHealthService> healthService,
      final CurrentUser currentUser)
  {
    this.jobDAO = jobDAO;
    this.jobEventDAO = jobEventDAO;
    this.healthDAO = healthDAO;
    this.indexService = indexService;
    this.healthService = healthService;
    this.currentUser = currentUser;
  }

  /**
   * Defaults an absent trigger and rejects anything outside the known set. An arbitrary value would
   * reach a varchar(40) column and surface as a 500 rather than a 400.
   */
  private static String resolveTrigger(final String trigger) {
    if (trigger == null || trigger.isBlank()) {
      return SearchIndexJob.TRIGGER_HEALTH_UI;
    }
    if (!SearchIndexJob.TRIGGERS.contains(trigger)) {
      throw new BadRequestException("Unsupported trigger: " + trigger);
    }
    return trigger;
  }

  public SearchIndexJob startJob(final String jobType, final String trigger) {
    if (jobType == null || jobType.isBlank()) {
      throw new BadRequestException("jobType is required");
    }
    if (UNIMPLEMENTED_JOB_TYPES.contains(jobType)) {
      throw new BadRequestException(
          "Job type " + jobType + " has no worker yet; run a FULL_REBUILD instead");
    }
    if (!SearchIndexJob.isRebuildType(jobType)) {
      throw new BadRequestException("Unsupported jobType: " + jobType);
    }
    String resolvedTrigger = resolveTrigger(trigger);

    synchronized (jobControlLock) {
      Optional<SearchIndexJob> active = jobDAO.findActiveJob();
      if (active.isPresent()) {
        throw new ConflictException("An index job is already active: " + active.get().getId());
      }
      // The pre-existing Advanced Search endpoints drive a rebuild without creating a job row, so an
      // empty active slot does not by itself mean the engine is idle. Scheduling on top of that would
      // run two rebuilds against one index.
      if (indexService.get().fullRebuildInProgress()) {
        throw new ConflictException(
            "A full index rebuild is already running outside the control plane; wait for it to finish");
      }

      Date now = new Date();
      SearchIndexJob job = new SearchIndexJob();
      job.setId(UUID.randomUUID().toString().replace("-", ""));
      job.setJobType(jobType);
      job.setTrigger(resolvedTrigger);
      job.setStatus(SearchIndexJob.STATUS_PENDING);
      job.setProgressPercent((short) 0);
      SearchIndexHealth health = healthDAO.getCurrent();
      if (health != null) {
        job.setRecommendedOp(health.getRecommendedOp());
        job.setServingGenerationIdAtStart(health.getServingGenerationId());
      }
      job.setCreatedByUserId(currentUser.getUsernameOrSystem());
      job.setCreatedAt(now);
      job.setUpdatedAt(now);
      try {
        jobDAO.insert(job);
      }
      catch (RuntimeException e) {
        // active_slot is uniquely constrained, so a node that loses the race fails here rather than
        // creating a second active job. Report it the same way as losing to the in-process check.
        if (ExceptionUtils.isDuplicateKeyException(e)) {
          throw new ConflictException("An index job is already active");
        }
        throw e;
      }

      appendEvent(job.getId(), SearchIndexJobEvent.SEVERITY_INFO, "STARTED",
          "Job queued: " + jobType);

      healthDAO.setActiveJobId(job.getId());

      job.setStatus(SearchIndexJob.STATUS_RUNNING);
      job.setPhase("REBUILDING");
      job.setStartedAt(now);
      job.setUpdatedAt(now);
      jobDAO.update(job);
      try {
        indexService.get().scheduleFullIndexCreation();
      }
      catch (RuntimeException e) {
        // Never leave RUNNING with active_job_id set — that blocks all future startJob calls.
        failJobAndClearActive(job, "SCHEDULE_FAILED", e.getMessage());
        throw e;
      }

      return jobDAO.getById(job.getId());
    }
  }

  public Optional<SearchIndexJob> getJob(final String jobId) {
    return Optional.ofNullable(jobDAO.getById(jobId));
  }

  public Optional<SearchIndexJob> getActiveJob() {
    return jobDAO.findActiveJob();
  }

  public List<SearchIndexJobEvent> getJobEvents(final String jobId, final int limit) {
    return jobEventDAO.listByJobId(jobId, limit);
  }

  public SearchIndexJob cancelActiveJob() {
    synchronized (jobControlLock) {
      SearchIndexJob job = jobDAO.findActiveJob()
          .orElseThrow(() -> new ConflictException("No active index job to cancel"));
      Date now = new Date();
      job.setStatus(SearchIndexJob.STATUS_CANCELLING);
      job.setCancelRequestedAt(now);
      job.setUpdatedAt(now);
      jobDAO.update(job);
      appendEvent(job.getId(), SearchIndexJobEvent.SEVERITY_WARN, "CANCEL_REQUESTED",
          "Cancel requested; keeping serving (blue) index");

      try {
        // Only rebuilds have an engine to stop; cancelling anything else must not reach into one.
        if (SearchIndexJob.isRebuildType(job.getJobType())) {
          indexService.get().cancelInFlightFullRebuild();
        }
        job.setStatus(SearchIndexJob.STATUS_CANCELLED);
        appendEvent(job.getId(), SearchIndexJobEvent.SEVERITY_INFO, "CANCELLED",
            "Job cancelled; blue index unchanged");
      }
      catch (RuntimeException e) {
        // Never leave CANCELLING — that blocks all future startJob calls.
        job.setStatus(SearchIndexJob.STATUS_FAILED);
        job.setErrorCode("CANCEL_FAILED");
        job.setErrorMessage(truncate(e.getMessage(), 2000));
        appendEvent(job.getId(), SearchIndexJobEvent.SEVERITY_ERROR, "CANCEL_FAILED",
            "Cancel request failed; job closed to unblock control plane");
        throw e;
      }
      finally {
        Date finished = new Date();
        job.setFinishedAt(finished);
        job.setUpdatedAt(finished);
        jobDAO.update(job);
        healthDAO.setActiveJobId(null);
      }
      return job;
    }
  }

  /**
   * Closes the active rebuild job when the engine finishes, either way, and releases the single
   * active slot.
   * <p>
   * Without this a successful rebuild never leaves RUNNING: {@code active_slot} keeps its constant,
   * which is uniquely constrained, so Analyze stays pinned to a rebuild that has finished and every
   * later start conflicts until someone cancels a job that actually succeeded.
   * <p>
   * A no-op when no control-plane job is active, because the pre-existing Advanced Search endpoints
   * drive the same engine without creating a job row.
   *
   * @param succeeded whether the engine finished the rebuild
   * @param errorMessage the failure detail, ignored when {@code succeeded}
   */
  public void onFullRebuildFinished(final boolean succeeded, final String errorMessage) {
    synchronized (jobControlLock) {
      Optional<SearchIndexJob> active = jobDAO.findActiveJob();
      if (active.isEmpty()) {
        return;
      }
      SearchIndexJob job = active.get();
      if (!SearchIndexJob.isRebuildType(job.getJobType())) {
        return;
      }
      // Cancel owns terminal state. The engine still runs to the end of its current work after a
      // cancel is requested, so it reports back here afterwards; recording that as SUCCEEDED would
      // contradict the CANCELLED the operator was already shown.
      if (SearchIndexJob.STATUS_CANCELLING.equals(job.getStatus())
          || SearchIndexJob.STATUS_CANCELLED.equals(job.getStatus()))
      {
        return;
      }

      Date now = new Date();
      if (succeeded) {
        job.setStatus(SearchIndexJob.STATUS_SUCCEEDED);
        job.setProgressPercent((short) 100);
        job.setPhase("COMPLETE");
        job.setErrorCode(null);
        job.setErrorMessage(null);
        // Every document has been rebuilt from source, so failures against the previous index no
        // longer describe this one. Left standing, the tally keeps the tenant unhealthy and keeps
        // recommending the rebuild that just finished.
        healthDAO.resetFailedChanges();
        appendEvent(job.getId(), SearchIndexJobEvent.SEVERITY_INFO, "SUCCEEDED",
            "Full index build completed");
      }
      else {
        job.setStatus(SearchIndexJob.STATUS_FAILED);
        job.setErrorCode("REBUILD_FAILED");
        job.setErrorMessage(truncate(errorMessage, 2000));
        appendEvent(job.getId(), SearchIndexJobEvent.SEVERITY_ERROR, "REBUILD_FAILED",
            "Full index build failed");
      }
      job.setFinishedAt(now);
      job.setUpdatedAt(now);
      jobDAO.update(job);
      healthDAO.setActiveJobId(null);
    }
    // Outside the lock: recomputing derived status reads the outbox, and it no longer needs to see
    // the job to get the right answer now that the slot is clear.
    healthService.get().refreshDerivedStatus();
  }

  private void failJobAndClearActive(
      final SearchIndexJob job,
      final String errorCode,
      final String errorMessage)
  {
    Date finished = new Date();
    job.setStatus(SearchIndexJob.STATUS_FAILED);
    job.setErrorCode(errorCode);
    job.setErrorMessage(truncate(errorMessage, 2000));
    job.setFinishedAt(finished);
    job.setUpdatedAt(finished);
    jobDAO.update(job);
    appendEvent(job.getId(), SearchIndexJobEvent.SEVERITY_ERROR, errorCode,
        "Job failed before engine accepted work; control plane unblocked");
    healthDAO.setActiveJobId(null);
  }

  private static String truncate(final String value, final int maxLen) {
    if (value == null) {
      return null;
    }
    return value.length() <= maxLen ? value : value.substring(0, maxLen);
  }

  /**
   * Records one line of a job's activity log. The log describes the operation rather than driving
   * it, so a failed write is swallowed: letting it propagate would let a lost connection strand a
   * job in CANCELLING, which {@code findActiveJob} counts as active and which therefore blocks every
   * later job until someone edits the database by hand.
   */
  private void appendEvent(
      final String jobId,
      final String severity,
      final String eventCode,
      final String message)
  {
    try {
      SearchIndexJobEvent event = new SearchIndexJobEvent();
      event.setId(UUID.randomUUID().toString().replace("-", ""));
      event.setSearchIndexJobId(jobId);
      event.setSeq(jobEventDAO.nextSeq(jobId));
      event.setSeverity(severity);
      event.setEventCode(eventCode);
      event.setMessage(message);
      event.setCreatedAt(new Date());
      jobEventDAO.insert(event);
    }
    catch (RuntimeException e) {
      log.warn("Could not record index job event {} for job {}", eventCode, jobId, e);
    }
  }
}
