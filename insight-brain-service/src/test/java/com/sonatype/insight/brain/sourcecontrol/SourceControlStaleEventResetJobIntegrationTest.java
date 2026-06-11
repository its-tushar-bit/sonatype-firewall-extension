/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_IN_PROGRESS;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.EVENT_STATUS_NEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link SourceControlStaleEventResetJob}. Drives the job's
 * {@link SourceControlStaleEventResetJob#executeForTenant(JobExecutionContext, Tenant)} entry point directly
 * against a real datastore, bypassing Quartz scheduling and the
 * {@link com.sonatype.insight.brain.tenancy.AllTenantsJob}
 * tenant-iteration framework (which is exercised by the dedicated MTIQ integration test).
 */
@Category(SlowTest.class)
public class SourceControlStaleEventResetJobIntegrationTest
    extends BrainInjectedTest
{
  private PerpetualLockManager perpetualLockManager;

  private SourceControlEventDAO sourceControlEventDAO;

  private SourceControlStaleEventResetJob job;

  @Before
  public void setUp() {
    OperationalDataStore ods = databaseContainerRule.getOperationalDataStore();
    perpetualLockManager = new PerpetualLockManager(new PerpetualLockDAO(ods));
    sourceControlEventDAO = new SourceControlEventDAO(ods);
    // We don't go through schedulePeriodicTask here; we drive executeForTenant() directly.
    TaskScheduler unusedTaskScheduler = mock(TaskScheduler.class);
    IqForScmLicenseChecker licenseChecker = mock(IqForScmLicenseChecker.class);
    when(licenseChecker.isIqForScmSupported()).thenReturn(true);
    ApiConfigFeaturesService apiConfigFeaturesService = mock(ApiConfigFeaturesService.class);
    when(apiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    job = new SourceControlStaleEventResetJob(unusedTaskScheduler, perpetualLockManager, sourceControlEventDAO,
        licenseChecker, apiConfigFeaturesService);
  }

  @Test
  public void execute_resetsEventStampedWithDeadInstanceButLeavesEventStampedWithLiveInstance() {
    // given: instance-LIVE has an active heartbeat row in perpetual_lock (category "source-control").
    perpetualLockManager.tryAcquireLock("instance-LIVE",
        SourceControlLoadBalancer.LOAD_BALANCER_CATEGORY_FOR_SCM, "instance-LIVE", 60);
    // and: instance-DEAD has no row (it crashed, never re-acquired).

    // and: two stuck `in_progress` events whose start_time is older than the cutoff window.
    SourceControlEvent staleAliveOwned = createStaleInProgressEvent("instance-LIVE");
    SourceControlEvent staleDeadOwned = createStaleInProgressEvent("instance-DEAD");

    // when: the job fires
    job.executeForTenant(mock(JobExecutionContext.class), Tenant.SINGLE_TENANT);

    // then: the live-owned event is left untouched (still in_progress, still owned).
    SourceControlEvent reloadedAlive = sourceControlEventDAO.getByIdNotNull(staleAliveOwned.getId());
    assertThat(reloadedAlive.getEventStatus()).isEqualTo(EVENT_STATUS_IN_PROGRESS);
    assertThat(reloadedAlive.getInstanceId()).isEqualTo("instance-LIVE");

    // and: the dead-owned event is reset (status back to new, instance cleared).
    SourceControlEvent reloadedDead = sourceControlEventDAO.getByIdNotNull(staleDeadOwned.getId());
    assertThat(reloadedDead.getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
    assertThat(reloadedDead.getInstanceId()).isNull();
  }

  @Test
  public void execute_resetsOrphanedInProgressEventWithNullInstanceId() {
    // The DAO has a separate clause that resets events with INSTANCE_ID IS NULL but EVENT_STATUS = 'in progress'
    // and START_TIME older than the cutoff. This can happen if a previous reset cycle cleared the instance_id
    // but the event was somehow re-marked in_progress, or via direct DB manipulation. Verify the new job
    // covers that branch end-to-end against the real DB.
    perpetualLockManager.tryAcquireLock("instance-LIVE",
        SourceControlLoadBalancer.LOAD_BALANCER_CATEGORY_FOR_SCM, "instance-LIVE", 60);

    SourceControlEvent orphanedEvent = createStaleInProgressEvent(null);

    job.executeForTenant(mock(JobExecutionContext.class), Tenant.SINGLE_TENANT);

    SourceControlEvent reloaded = sourceControlEventDAO.getByIdNotNull(orphanedEvent.getId());
    assertThat(reloaded.getEventStatus()).isEqualTo(EVENT_STATUS_NEW);
    assertThat(reloaded.getInstanceId()).isNull();
  }

  @Test
  public void execute_doesNothingWhenNoHeartbeatsExist() {
    // No perpetual_lock rows in 'source-control' — should NOT call resetStaleEvents at all (the empty-set
    // sentinel path in the DAO would otherwise reset every stale-looking event in the system).
    SourceControlEvent staleDeadOwned = createStaleInProgressEvent("instance-DEAD");

    job.executeForTenant(mock(JobExecutionContext.class), Tenant.SINGLE_TENANT);

    SourceControlEvent reloaded = sourceControlEventDAO.getByIdNotNull(staleDeadOwned.getId());
    assertThat(reloaded.getEventStatus())
        .as("With zero live heartbeats, the job must short-circuit and NOT reset any events. The previous "
            + "behavior (DAO substitutes an invalid sentinel id and resets every stale-looking event) is "
            + "intentionally suppressed.")
        .isEqualTo(EVENT_STATUS_IN_PROGRESS);
    assertThat(reloaded.getInstanceId()).isEqualTo("instance-DEAD");
  }

  // ---- helpers -------------------------------------------------------------

  private SourceControlEvent createStaleInProgressEvent(String instanceId) {
    Application application = tempEntity.newApplicationWithParent();
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), BuildStageType.ID, "sourceScan", "sourceCommit");
    // Build the event directly so we can stamp create_time / start_time / event_status / instance_id
    // explicitly. tempEntity.newSourceControlEvent uses factory defaults that won't satisfy
    // resetStaleEvents' START_TIME < cutoff filter for in_progress.
    long now = System.currentTimeMillis();
    Date wellBeforeCutoff =
        new Date(now - (SourceControlStaleEventResetJob.STALE_EVENT_CUTOFF_SECONDS + 60) * 1_000L);
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId(application.getId())
        .setCommitHash("abcdefg")
        .setEventType(SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT)
        .setPolicyEvaluationId(policyEvaluation.getId())
        .setBranchName("branch")
        .setEventStatus(EVENT_STATUS_IN_PROGRESS)
        .setPullRequestNumber(2)
        .setScmUsername("user-X")
        .setInitiator("webhook")
        .setInstanceId(instanceId)
        .setCreateTime(wellBeforeCutoff)
        .setStartTime(wellBeforeCutoff);
    sourceControlEventDAO.insert(event);
    return event;
  }
}
