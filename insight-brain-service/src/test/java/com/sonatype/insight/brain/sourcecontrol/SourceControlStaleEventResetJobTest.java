/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SourceControlStaleEventResetJobTest
{
  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private PerpetualLockManager perpetualLockManager;

  @Mock
  private SourceControlEventDAO sourceControlEventDAO;

  @Mock
  private IqForScmLicenseChecker licenseChecker;

  @Mock
  private ApiConfigFeaturesService apiConfigFeaturesService;

  private SourceControlStaleEventResetJob job;

  private AutoCloseable mockCloser;

  @BeforeEach
  public void setUp() {
    mockCloser = MockitoAnnotations.openMocks(this);
    // Default to "licensed and feature-enabled" — individual tests override as needed.
    when(licenseChecker.isIqForScmSupported()).thenReturn(true);
    when(apiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    job = new SourceControlStaleEventResetJob(taskScheduler, perpetualLockManager, sourceControlEventDAO,
        licenseChecker, apiConfigFeaturesService);
  }

  @AfterEach
  public void tearDown() throws Exception {
    // Reset the static tenant ThreadLocal that some tests in this class flip into multi-tenant mode,
    // so a flip doesn't bleed into the next test class on the same JVM.
    TenantTestHelper.resetAfterTest();
    mockCloser.close();
  }

  @Test
  public void register_schedulesPeriodicTaskAt15SecondInterval() {
    job.register();

    verify(taskScheduler).schedulePeriodicTask(job, SourceControlStaleEventResetJob.PERIOD);
    verifyNoInteractions(perpetualLockManager, sourceControlEventDAO);
  }

  @Test
  public void register_isNoopWhenDisabledForTesting() {
    job.disableForTesting = true;

    job.register();

    verify(taskScheduler, never()).schedulePeriodicTask(job, SourceControlStaleEventResetJob.PERIOD);
  }

  @Test
  public void disallowConcurrentExecution_isSetOnTheJobClass() {
    // Quartz needs the @DisallowConcurrentExecution annotation present on the actual
    // job class so that, in clustered mode, two nodes never fire the trigger at once.
    assertThat(JobBuilder.newJob(SourceControlStaleEventResetJob.class).build().isConcurrentExectionDisallowed())
        .as("@DisallowConcurrentExecution must remain on the job class. In clustered Quartz mode, this is "
            + "what prevents two nodes from racing on stale-event reset; without it, the cluster-wide "
            + "single-fire guarantee is gone.")
        .isTrue();
  }

  @Test
  public void executeForTenant_resetsStaleEventsUsingFreshlyReadActiveInstanceIds() {
    // given: three live perpetual_lock rows in the source-control category,
    // owned by two distinct instances (one row is a heartbeat, another is a partition reservation,
    // and a third is a heartbeat for a second instance).
    PerpetualLock heartbeatA = activeLock("instance-A", "instance-A");
    PerpetualLock partitionForA = activeLock("partition-key-1", "instance-A");
    PerpetualLock heartbeatB = activeLock("instance-B", "instance-B");
    when(perpetualLockManager.getAllActivePerpetualLocksForCategory(
        SourceControlLoadBalancer.LOAD_BALANCER_CATEGORY_FOR_SCM))
            .thenReturn(List.of(heartbeatA, partitionForA, heartbeatB));

    // when: the job executes for a tenant
    job.executeForTenant(mock(JobExecutionContext.class), Tenant.SINGLE_TENANT);

    // then: the DAO is called with the deduped owner set and the configured cutoff.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<String>> activeIds = ArgumentCaptor.forClass(Set.class);
    verify(sourceControlEventDAO).resetStaleEvents(
        activeIds.capture(),
        eq(SourceControlStaleEventResetJob.STALE_EVENT_CUTOFF_SECONDS));
    assertThat(activeIds.getValue()).containsExactlyInAnyOrder("instance-A", "instance-B");
  }

  @Test
  public void executeForTenant_skipsDaoWhenNoLiveLocksExist() {
    // SourceControlEventDAO.resetStaleEvents, on an empty set, substitutes an invalid sentinel id and
    // proceeds to reset every stale-looking event in the system -- including events stamped by alive
    // instances that just haven't refreshed their heartbeat row yet. That blast radius is not
    // acceptable, so the job short-circuits and waits for the next cycle instead of calling the DAO.
    when(perpetualLockManager.getAllActivePerpetualLocksForCategory(
        SourceControlLoadBalancer.LOAD_BALANCER_CATEGORY_FOR_SCM))
            .thenReturn(List.of());

    job.executeForTenant(mock(JobExecutionContext.class), Tenant.SINGLE_TENANT);

    verifyNoInteractions(sourceControlEventDAO);
  }

  @Test
  public void executeForTenant_filtersOutNullOwnersFromTheActiveInstanceSet() {
    // Defensive filter: the active-locks query (PerpetualLockDAO.getAllActivePartitionLocksForCategory)
    // filters on EXPIRATION_TIME > now but does not constrain OWNER. If a future change ever surfaces a
    // row with a non-null expiration and a null owner, we don't want `null` ending up in the
    // active-instance-id set passed to the DAO.
    PerpetualLock alive = activeLock("instance-A", "instance-A");
    PerpetualLock nullOwnerRow = activeLock("some-key", null);
    when(perpetualLockManager.getAllActivePerpetualLocksForCategory(
        SourceControlLoadBalancer.LOAD_BALANCER_CATEGORY_FOR_SCM))
            .thenReturn(List.of(alive, nullOwnerRow));

    job.executeForTenant(mock(JobExecutionContext.class), Tenant.SINGLE_TENANT);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<String>> activeIds = ArgumentCaptor.forClass(Set.class);
    verify(sourceControlEventDAO).resetStaleEvents(
        activeIds.capture(),
        eq(SourceControlStaleEventResetJob.STALE_EVENT_CUTOFF_SECONDS));
    assertThat(activeIds.getValue()).containsExactly("instance-A");
  }

  @Test
  public void resetStaleEvents_readsPerpetualLockUnderGlobalTenant_butCallsDaoUnderCallingTenant() {
    // The lock category 'source-control' lives in the global schema in MTIQ, so the perpetual_lock
    // read must happen under Tenant.GLOBAL_TENANT. In contrast, source_control_event is per-tenant in
    // MTIQ, so the DAO call must happen under the *calling* tenant — wrapping it in runAsGlobal would
    // route the UPDATE to the global schema and silently miss the tenant's stuck events.
    //
    // Multi-tenant mode must be initialized AND a non-SINGLE_TENANT must be active for runAsGlobal to
    // actually flip the thread-local — see TenantThreadLocal.runAsWithoutValidation. Otherwise the
    // assertions below would pass trivially whether or not the wrapper exists / is correctly placed.
    TenantTestHelper.initMultiTenantMode();
    AtomicReference<Throwable> capturedFailure = new AtomicReference<>();
    PerpetualLock alive = activeLock("instance-A", "instance-A");

    // Capture the tenant context at the moment of the perpetual_lock read.
    when(perpetualLockManager.getAllActivePerpetualLocksForCategory(
        SourceControlLoadBalancer.LOAD_BALANCER_CATEGORY_FOR_SCM))
            .thenAnswer(invocation -> {
              try {
                TenantTestHelper.assertTenantSet(Tenant.GLOBAL_TENANT);
              }
              catch (Throwable t) {
                capturedFailure.set(t);
              }
              return List.of(alive);
            });

    Tenant testTenant = TenantTestHelper.testAsNewTenant("scm-stale-event-reset-test-tenant", tenant -> {
      // Sanity-check: at the entry point we are NOT global. If runAsGlobal were missing on the
      // perpetual_lock read, the assertion above would observe `tenant` instead of GLOBAL_TENANT.
      TenantTestHelper.assertTenantSet(tenant);

      // Capture the tenant context at the moment of the DAO call. resetStaleEvents returns void, so
      // we use doAnswer rather than when().thenAnswer.
      doAnswer(invocation -> {
        try {
          TenantTestHelper.assertTenantSet(tenant);
        }
        catch (Throwable t) {
          capturedFailure.set(t);
        }
        return null;
      }).when(sourceControlEventDAO).resetStaleEvents(any(), anyInt());

      job.resetStaleEvents();
    });

    assertThat(capturedFailure.get())
        .as("perpetual_lock read must run under Tenant.GLOBAL_TENANT (global schema), and "
            + "SourceControlEventDAO.resetStaleEvents must run under the calling per-tenant context "
            + "(per-tenant schema). %s",
            testTenant)
        .isNull();
  }

  @Test
  public void executeForTenant_doesNotPropagateExceptions() {
    // AllTenantsJob.execute does not catch exceptions from executeForTenant; if one tenant's reset
    // throws, the next tenant's reset must still get a chance to run. So executeForTenant catches
    // and logs locally.
    doThrow(new RuntimeException("simulated DB failure")).when(perpetualLockManager)
        .getAllActivePerpetualLocksForCategory(SourceControlLoadBalancer.LOAD_BALANCER_CATEGORY_FOR_SCM);

    job.executeForTenant(mock(JobExecutionContext.class), Tenant.SINGLE_TENANT);

    verify(sourceControlEventDAO, never()).resetStaleEvents(any(), anyInt());
  }

  @Test
  public void isLicensed_returnsTrueWhenScmSupportedAndSaasLifecycleScmEnabled() {
    when(licenseChecker.isIqForScmSupported()).thenReturn(true);
    when(apiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);

    assertThat(job.isLicensed()).isTrue();
  }

  @Test
  public void isLicensed_returnsFalseWhenScmIsNotLicensed() {
    when(licenseChecker.isIqForScmSupported()).thenReturn(false);
    when(apiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);

    assertThat(job.isLicensed()).isFalse();
  }

  @Test
  public void isLicensed_returnsFalseWhenSaasLifecycleScmIsNotEnabled() {
    when(licenseChecker.isIqForScmSupported()).thenReturn(true);
    when(apiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(false);

    assertThat(job.isLicensed()).isFalse();
  }

  @Test
  public void executeForTenant_skipsAllWorkWhenUnlicensed() {
    // AllTenantsJob.execute consults isLicensed() per-tenant in the MTIQ-batch branch, but NOT in the
    // single-tenant branch. executeForTenant therefore consults isLicensed() itself so the prior
    // SourceControlEventOrchestrator gate is preserved on both on-prem (single-tenant) and MTIQ.
    when(licenseChecker.isIqForScmSupported()).thenReturn(false);

    job.executeForTenant(mock(JobExecutionContext.class), Tenant.SINGLE_TENANT);

    verifyNoInteractions(perpetualLockManager, sourceControlEventDAO);
  }

  private static PerpetualLock activeLock(String id, String owner) {
    PerpetualLock lock = new PerpetualLock("source-control", id);
    lock.setOwner(owner);
    lock.setExpirationTime(new Date(System.currentTimeMillis() + 60_000));
    return lock;
  }
}
