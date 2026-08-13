/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Provider;

import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexHealthDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexJobDAO;
import com.sonatype.insight.brain.dataaccess.searchindex.SearchIndexJobEventDAO;
import com.sonatype.insight.brain.model.searchindex.SearchIndexHealth;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJob;
import com.sonatype.insight.brain.model.searchindex.SearchIndexJobEvent;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchIndexJobServiceTest
{
  @Mock
  private SearchIndexJobDAO jobDAO;

  @Mock
  private SearchIndexJobEventDAO jobEventDAO;

  @Mock
  private SearchIndexHealthDAO healthDAO;

  @Mock
  private IndexService indexService;

  @Mock
  private SearchIndexHealthService healthService;

  @Mock
  private CurrentUser currentUser;

  private SearchIndexJobService service;

  @BeforeEach
  public void setUp() {
    Provider<IndexService> indexServiceProvider = () -> indexService;
    Provider<SearchIndexHealthService> healthServiceProvider = () -> healthService;
    service = new SearchIndexJobService(jobDAO, jobEventDAO, healthDAO, indexServiceProvider,
        healthServiceProvider, currentUser);
    lenient().when(currentUser.getUsernameOrSystem()).thenReturn("admin");
    lenient().when(jobEventDAO.nextSeq(anyString())).thenReturn(1L);
    lenient().when(jobDAO.getById(anyString())).thenAnswer(invocation -> {
      SearchIndexJob job = new SearchIndexJob();
      job.setId(invocation.getArgument(0));
      job.setStatus(SearchIndexJob.STATUS_RUNNING);
      return job;
    });
  }

  @Test
  public void startJob_fullRebuild_invokesCreateIndexAsync() {
    when(jobDAO.findActiveJob()).thenReturn(Optional.empty());
    SearchIndexHealth health = new SearchIndexHealth();
    health.setRecommendedOp(SearchIndexHealth.OP_FULL_REBUILD);
    health.setServingGenerationId("gen-blue");
    when(healthDAO.getCurrent()).thenReturn(health);

    SearchIndexJob job = service.startJob(SearchIndexJob.TYPE_FULL_REBUILD, SearchIndexJob.TRIGGER_HEALTH_UI);

    assertThat(job.getId()).isNotBlank();
    verify(indexService).scheduleFullIndexCreation();
    verify(healthDAO).setActiveJobId(anyString());
    ArgumentCaptor<SearchIndexJob> jobCaptor = ArgumentCaptor.forClass(SearchIndexJob.class);
    verify(jobDAO).insert(jobCaptor.capture());
    assertThat(jobCaptor.getValue().getServingGenerationIdAtStart()).isEqualTo("gen-blue");
  }

  /**
   * Accepting a maintenance type would mark it RUNNING with no worker to finish it, so it stays the
   * active job and every later start conflicts. Reject until the worker lands (CLM-44498).
   */
  @Test
  public void startJob_rejectsMaintenanceTypesThatHaveNoWorker() {
    for (String jobType : List.of(SearchIndexJob.TYPE_SCOPED_CLEANUP, SearchIndexJob.TYPE_POINT_REPAIR,
        SearchIndexJob.TYPE_ATTRIBUTE_BACKFILL))
    {
      assertThatThrownBy(() -> service.startJob(jobType, SearchIndexJob.TRIGGER_HEALTH_UI))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining(jobType);
    }

    verify(indexService, never()).scheduleFullIndexCreation();
    verify(jobDAO, never()).insert(any(SearchIndexJob.class));
    verify(healthDAO, never()).setActiveJobId(anyString());
  }

  @Test
  public void startJob_rejectsUnknownType() {
    assertThatThrownBy(() -> service.startJob("NOPE", SearchIndexJob.TRIGGER_SYSTEM))
        .isInstanceOf(BadRequestException.class);
  }

  // Set.of() throws NPE rather than returning false, so null has to be rejected before any lookup.
  @Test
  public void startJob_rejectsNullTypeWithoutThrowingFromASetLookup() {
    assertThatThrownBy(() -> service.startJob(null, SearchIndexJob.TRIGGER_SYSTEM))
        .isInstanceOf(BadRequestException.class);
  }

  /**
   * The JVM lock only serializes one node. A second node passing the active-job check loses on the
   * active_slot unique constraint instead, and that has to read as a conflict rather than a 500.
   */
  @Test
  public void startJob_reportsALostRaceOnTheUniqueSlotAsAConflict() {
    when(jobDAO.findActiveJob()).thenReturn(Optional.empty());
    when(healthDAO.getCurrent()).thenReturn(new SearchIndexHealth());
    doThrow(new DataAccessException("Unique index or primary key violation: search_index_job_active_slot_uk"))
        .when(jobDAO)
        .insert(any(SearchIndexJob.class));

    assertThatThrownBy(() -> service.startJob(SearchIndexJob.TYPE_FULL_REBUILD, SearchIndexJob.TRIGGER_HEALTH_UI))
        .isInstanceOf(ConflictException.class);

    verify(indexService, never()).scheduleFullIndexCreation();
  }

  /**
   * The legacy Advanced Search endpoints rebuild without creating a job row, so an empty active slot
   * does not mean the engine is idle. Starting anyway would run two rebuilds against one index.
   */
  @Test
  public void startJob_rejectsWhenARebuildIsAlreadyRunningOutsideTheControlPlane() {
    when(jobDAO.findActiveJob()).thenReturn(Optional.empty());
    when(indexService.fullRebuildInProgress()).thenReturn(true);

    assertThatThrownBy(() -> service.startJob(SearchIndexJob.TYPE_FULL_REBUILD, SearchIndexJob.TRIGGER_HEALTH_UI))
        .isInstanceOf(ConflictException.class);

    verify(jobDAO, never()).insert(any(SearchIndexJob.class));
    verify(indexService, never()).scheduleFullIndexCreation();
    verify(healthDAO, never()).setActiveJobId(anyString());
  }

  /** An arbitrary trigger reaches a varchar(40) column, so it has to fail as a 400 not a 500. */
  @Test
  public void startJob_rejectsATriggerOutsideTheKnownSet() {
    assertThatThrownBy(() -> service.startJob(SearchIndexJob.TYPE_FULL_REBUILD, "x".repeat(64)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("trigger");

    verify(jobDAO, never()).insert(any(SearchIndexJob.class));
    verify(indexService, never()).scheduleFullIndexCreation();
  }

  @Test
  public void startJob_defaultsAnAbsentTriggerToTheHealthUi() {
    when(jobDAO.findActiveJob()).thenReturn(Optional.empty());
    when(healthDAO.getCurrent()).thenReturn(new SearchIndexHealth());

    service.startJob(SearchIndexJob.TYPE_FULL_REBUILD, null);

    ArgumentCaptor<SearchIndexJob> jobCaptor = ArgumentCaptor.forClass(SearchIndexJob.class);
    verify(jobDAO).insert(jobCaptor.capture());
    assertThat(jobCaptor.getValue().getTrigger()).isEqualTo(SearchIndexJob.TRIGGER_HEALTH_UI);
  }

  @Test
  public void startJob_rejectsWhenActiveJobExists() {
    SearchIndexJob active = new SearchIndexJob();
    active.setId("busy");
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(active));

    assertThatThrownBy(() -> service.startJob(SearchIndexJob.TYPE_FULL_REBUILD, SearchIndexJob.TRIGGER_SYSTEM))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  public void cancelActiveJob_cancelsRebuildAndClearsActive() {
    SearchIndexJob active = new SearchIndexJob();
    active.setId("job-1");
    active.setJobType(SearchIndexJob.TYPE_FULL_REBUILD);
    active.setStatus(SearchIndexJob.STATUS_RUNNING);
    active.setCreatedAt(new Date());
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(active));

    SearchIndexJob cancelled = service.cancelActiveJob();

    assertThat(cancelled.getStatus()).isEqualTo(SearchIndexJob.STATUS_CANCELLED);
    verify(indexService).cancelInFlightFullRebuild();
    verify(healthDAO).setActiveJobId(null);
  }

  @Test
  public void cancelActiveJob_onEngineFailure_closesJobSoControlPlaneUnblocks() {
    SearchIndexJob active = new SearchIndexJob();
    active.setId("job-1");
    active.setJobType(SearchIndexJob.TYPE_FULL_REBUILD);
    active.setStatus(SearchIndexJob.STATUS_RUNNING);
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(active));
    doThrow(new RuntimeException("boom")).when(indexService).cancelInFlightFullRebuild();

    assertThatThrownBy(() -> service.cancelActiveJob()).isInstanceOf(RuntimeException.class);

    assertThat(active.getStatus()).isEqualTo(SearchIndexJob.STATUS_FAILED);
    verify(healthDAO).setActiveJobId(null);
  }

  /**
   * The activity log is a description of the cancel, not part of it. A log write that fails must not
   * strand the job in CANCELLING, which counts as active and would block every later job, nor report
   * a rebuild that stopped cleanly as CANCEL_FAILED.
   */
  @Test
  public void cancelActiveJob_stillClosesWhenTheActivityLogCannotBeWritten() {
    SearchIndexJob active = new SearchIndexJob();
    active.setId("job-1");
    active.setJobType(SearchIndexJob.TYPE_FULL_REBUILD);
    active.setStatus(SearchIndexJob.STATUS_RUNNING);
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(active));
    doThrow(new RuntimeException("connection lost")).when(jobEventDAO).insert(any(SearchIndexJobEvent.class));

    SearchIndexJob cancelled = service.cancelActiveJob();

    assertThat(cancelled.getStatus()).isEqualTo(SearchIndexJob.STATUS_CANCELLED);
    assertThat(cancelled.getErrorCode()).isNull();
    verify(healthDAO).setActiveJobId(null);
  }

  /**
   * Only a rebuild has an engine to stop. A job of any other type must still close cleanly rather
   * than reaching into the full-rebuild cancel path.
   */
  @Test
  public void cancelActiveJob_nonRebuildTypeDoesNotTouchTheRebuildEngine() {
    SearchIndexJob active = new SearchIndexJob();
    active.setId("job-1");
    active.setJobType(SearchIndexJob.TYPE_SCOPED_CLEANUP);
    active.setStatus(SearchIndexJob.STATUS_RUNNING);
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(active));

    SearchIndexJob cancelled = service.cancelActiveJob();

    assertThat(cancelled.getStatus()).isEqualTo(SearchIndexJob.STATUS_CANCELLED);
    verify(indexService, never()).cancelInFlightFullRebuild();
    verify(healthDAO).setActiveJobId(null);
  }

  /**
   * The slot is what makes this matter rather than just the status: {@code active_slot} is uniquely
   * constrained, so a rebuild that finishes without being closed here holds it until someone cancels a
   * job that already succeeded, and Analyze keeps reporting a rebuild that is over.
   */
  @Test
  public void onFullRebuildFinished_successClosesTheJobAndReleasesTheActiveSlot() {
    SearchIndexJob active = runningRebuild();
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(active));

    service.onFullRebuildFinished(true, null);

    assertThat(active.getStatus()).isEqualTo(SearchIndexJob.STATUS_SUCCEEDED);
    assertThat(active.getActiveSlot()).isNull();
    assertThat(active.getProgressPercent()).isEqualTo((short) 100);
    assertThat(active.getFinishedAt()).isNotNull();
    verify(healthDAO).setActiveJobId(null);
    verify(healthService).refreshDerivedStatus();
  }

  /**
   * The failed-change tally counts failures against the index that was just replaced. Left standing it
   * keeps the tenant unhealthy and keeps recommending the rebuild that has only now finished.
   */
  @Test
  public void onFullRebuildFinished_successClearsFailuresRecordedAgainstThePreviousIndex() {
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(runningRebuild()));

    service.onFullRebuildFinished(true, null);

    verify(healthDAO).resetFailedChanges();
  }

  @Test
  public void onFullRebuildFinished_failureRecordsTheReasonAndStillReleasesTheSlot() {
    SearchIndexJob active = runningRebuild();
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(active));

    service.onFullRebuildFinished(false, "disk full");

    assertThat(active.getStatus()).isEqualTo(SearchIndexJob.STATUS_FAILED);
    assertThat(active.getActiveSlot()).isNull();
    assertThat(active.getErrorCode()).isEqualTo("REBUILD_FAILED");
    assertThat(active.getErrorMessage()).isEqualTo("disk full");
    verify(healthDAO).setActiveJobId(null);
    verify(healthDAO, never()).resetFailedChanges();
  }

  /**
   * The pre-existing Advanced Search endpoints drive the same engine without creating a job row, so
   * the engine reporting back has to be harmless when the control plane never started anything.
   */
  @Test
  public void onFullRebuildFinished_isANoOpWhenNoControlPlaneJobIsActive() {
    when(jobDAO.findActiveJob()).thenReturn(Optional.empty());

    service.onFullRebuildFinished(true, null);

    verify(jobDAO, never()).update(any(SearchIndexJob.class));
    verify(healthDAO, never()).setActiveJobId(any());
    verify(healthService, never()).refreshDerivedStatus();
  }

  /**
   * A cancelled rebuild still runs to the end of its current work and reports back afterwards. Taking
   * that as success would contradict the CANCELLED the operator was already shown.
   */
  @Test
  public void onFullRebuildFinished_leavesACancelledJobAlone() {
    SearchIndexJob active = runningRebuild();
    active.setStatus(SearchIndexJob.STATUS_CANCELLING);
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(active));

    service.onFullRebuildFinished(true, null);

    assertThat(active.getStatus()).isEqualTo(SearchIndexJob.STATUS_CANCELLING);
    verify(jobDAO, never()).update(any(SearchIndexJob.class));
  }

  @Test
  public void onFullRebuildFinished_ignoresAJobTheRebuildEngineDoesNotOwn() {
    SearchIndexJob active = runningRebuild();
    active.setJobType(SearchIndexJob.TYPE_SCOPED_CLEANUP);
    when(jobDAO.findActiveJob()).thenReturn(Optional.of(active));

    service.onFullRebuildFinished(true, null);

    assertThat(active.getStatus()).isEqualTo(SearchIndexJob.STATUS_RUNNING);
    verify(jobDAO, never()).update(any(SearchIndexJob.class));
  }

  private static SearchIndexJob runningRebuild() {
    SearchIndexJob job = new SearchIndexJob();
    job.setId("job-1");
    job.setJobType(SearchIndexJob.TYPE_FULL_REBUILD);
    job.setStatus(SearchIndexJob.STATUS_RUNNING);
    job.setCreatedAt(new Date());
    return job;
  }

  @Test
  public void startJob_onScheduleFailure_closesJobSoControlPlaneUnblocks() {
    when(jobDAO.findActiveJob()).thenReturn(Optional.empty());
    when(healthDAO.getCurrent()).thenReturn(new SearchIndexHealth());
    doThrow(new RuntimeException("schedule boom")).when(indexService).scheduleFullIndexCreation();

    assertThatThrownBy(() -> service.startJob(SearchIndexJob.TYPE_FULL_REBUILD, SearchIndexJob.TRIGGER_HEALTH_UI))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("schedule boom");

    ArgumentCaptor<SearchIndexJob> jobCaptor = ArgumentCaptor.forClass(SearchIndexJob.class);
    verify(jobDAO, times(2)).update(jobCaptor.capture());
    SearchIndexJob failed = jobCaptor.getAllValues().get(1);
    assertThat(failed.getStatus()).isEqualTo(SearchIndexJob.STATUS_FAILED);
    assertThat(failed.getErrorCode()).isEqualTo("SCHEDULE_FAILED");
    verify(healthDAO).setActiveJobId(null);
  }
}
