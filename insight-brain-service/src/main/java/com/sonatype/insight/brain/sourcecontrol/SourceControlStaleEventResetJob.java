/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.concurrent.PerpetualLockManager;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.AllTenantsJob;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.google.common.annotations.VisibleForTesting;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically resets SCM events that are stuck in {@code in_progress} on instances that are no longer alive,
 * so other instances can pick them up. The "run on exactly one cluster node per cycle" property is provided
 * by Quartz clustering ({@link DisallowConcurrentExecution} plus a clustered {@code JobStoreTX}).
 *
 * <p>
 * The set of "live" instance IDs is read fresh each cycle from the {@code perpetual_lock} rows in the
 * {@link SourceControlLoadBalancer#LOAD_BALANCER_CATEGORY_FOR_SCM} category, which the heartbeat /
 * partition-reservation code maintains. That table lives in the global schema in MTIQ.
 *
 * <p>
 * The {@code source_control_event} table, however, is per-tenant in MTIQ — each tenant has its own copy.
 * So this job implements {@link AllTenantsJob}: Quartz fires the trigger once on a single batch node per
 * cycle, then the framework iterates over all tenants and runs {@link #executeForTenant} under each
 * tenant's thread-local context. Inside that method, the {@code perpetual_lock} read is wrapped in
 * {@link TenantThreadLocal#runAsGlobal} so the read targets the global schema, while the DAO update
 * runs back under the per-tenant context so it targets that tenant's {@code source_control_event} table.
 *
 * <p>
 * Behavior changes vs. the prior in-line cleanup that ran inside
 * {@code SourceControlLoadBalancer.acquireEventsToProcess()}:
 * <ul>
 * <li>Coordination moves from a custom {@code source-control-maintenance} {@code perpetual_lock} mutex
 * to standard clustered Quartz scheduling.</li>
 * <li>If no heartbeat rows are visible at the moment of a cycle, this job logs a warning and skips
 * rather than letting the DAO substitute its empty-set sentinel and reset every in-progress event.
 * The next 15s tick re-checks. The old code did not have this guard.</li>
 * </ul>
 * The user-visible behavior — stuck events on dead instances become eligible to be reclaimed within
 * the same window — is preserved.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class SourceControlStaleEventResetJob
    implements InsightJob, AllTenantsJob
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlStaleEventResetJob.class);

  @VisibleForTesting
  static final String TASK_NAME = "SourceControlStaleEventResetJob";

  @VisibleForTesting
  static final Duration PERIOD = Duration.ofSeconds(15);

  // PERIOD intentionally duplicates the value of
  // SourceControlLoadBalancer.SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS rather than referencing
  // it: that constant governs the load balancer's own event-acquisition cadence, which is a distinct
  // concept from this cleanup job's tick rate. They happen to coincide today; either may be tuned
  // independently in the future.

  // Two minutes is long enough for transient slowness on an alive instance to not look stale, but short
  // enough that work assigned to a dead instance is reclaimed promptly.
  //
  // The meaningful invariant is STALE_EVENT_CUTOFF > the SCM heartbeat lock TTL,
  // SourceControlLoadBalancer.SOURCE_CONTROL_INSTANCE_RESERVATION_SECONDS (currently 65s — set as
  // PullRequestPollingScheduler.PULL_REQUEST_DISCOVERY_INTERVAL_SECONDS + 5). A live instance's
  // heartbeat row in perpetual_lock is held for that many seconds and refreshed every (TTL - 5)s.
  // If the cutoff were <= TTL, a momentary heartbeat-refresh race could remove a live instance from
  // the active-instance-id set while it still has events stamped newer than the cutoff, and we'd
  // reset events the live instance is actively processing. Two minutes (~1.85x TTL) leaves comfortable
  // margin for refresh jitter without delaying dead-instance reclaim.
  @VisibleForTesting
  static final Duration STALE_EVENT_CUTOFF = Duration.ofMinutes(2);

  // SourceControlEventDAO.resetStaleEvents takes an int seconds; derive from the Duration above so the
  // two cannot drift.
  @VisibleForTesting
  static final int STALE_EVENT_CUTOFF_SECONDS = Math.toIntExact(STALE_EVENT_CUTOFF.toSeconds());

  private final TaskScheduler taskScheduler;

  private final PerpetualLockManager perpetualLockManager;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final IqForScmLicenseChecker licenseChecker;

  private final ApiConfigFeaturesService apiConfigFeaturesService;

  public boolean disableForTesting;

  @Inject
  public SourceControlStaleEventResetJob(
      TaskScheduler taskScheduler,
      PerpetualLockManager perpetualLockManager,
      SourceControlEventDAO sourceControlEventDAO,
      IqForScmLicenseChecker licenseChecker,
      ApiConfigFeaturesService apiConfigFeaturesService)
  {
    this.taskScheduler = taskScheduler;
    this.perpetualLockManager = perpetualLockManager;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.licenseChecker = licenseChecker;
    this.apiConfigFeaturesService = apiConfigFeaturesService;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.schedulePeriodicTask(this, PERIOD);
  }

  @Override
  public void deregister() {
    // Do not unschedule task otherwise it will break MTIQ - SDEV-1312
  }

  /**
   * Mirrors the licensing / feature gate the prior implementation enforced via
   * {@code SourceControlEventOrchestrator.fetchAndRouteEvents()}.
   *
   * <p>
   * Note: {@link AllTenantsJob#execute(JobExecutionContext)} only consults {@code isLicensed()} in
   * the MTIQ-batch branch, NOT in the single-tenant branch. To preserve the prior gate in both
   * deployment modes, {@link #executeForTenant} also consults this method directly.
   */
  @Override
  public boolean isLicensed() {
    return licenseChecker.isIqForScmSupported() && apiConfigFeaturesService.isSaasLifecycleScmEnabled();
  }

  /**
   * Runs once per tenant per cycle (and once per cycle in non-MTIQ deployments). The thread-local
   * tenant has already been set to {@code tenant} by the {@link AllTenantsJob} iteration framework.
   *
   * <p>
   * Exception handling is local — neither {@link AllTenantsJob#execute(JobExecutionContext)} nor
   * {@code Quartz} swallow exceptions from {@code executeForTenant}, so we catch here to avoid
   * marking the trigger as misfired on a single tenant's failure.
   */
  @Override
  public void executeForTenant(JobExecutionContext context, Tenant tenant) {
    if (!isLicensed()) {
      // Preserve the prior in-orchestrator gate. AllTenantsJob.execute already consults isLicensed
      // in the MTIQ-batch branch, so this is a no-op redundancy there; in single-tenant mode it is
      // the sole gate.
      return;
    }
    try (MDCUsernameScope scope = MDCUsernameScope.forSystem()) {
      resetStaleEvents();
    }
    catch (Exception e) {
      log.error("Failed to reset stale source control events for tenant {}: {}",
          tenant.tenantSlug, e.getMessage(), e);
    }
  }

  /**
   * Reads the cluster-wide active-instance-id set from the global {@code perpetual_lock} table, then
   * (still under the calling per-tenant thread-local context) asks the DAO to reset stale
   * {@code in_progress} events for that tenant.
   */
  @VisibleForTesting
  void resetStaleEvents() {
    // perpetual_lock is a global-schema table, so the read must happen under the global tenant.
    Set<String> activeInstanceIds = TenantThreadLocal.runAsGlobal(() -> perpetualLockManager
        .getAllActivePerpetualLocksForCategory(SourceControlLoadBalancer.LOAD_BALANCER_CATEGORY_FOR_SCM)
        .stream()
        .map(PerpetualLock::getOwner)
        .filter(owner -> owner != null && !owner.isBlank())
        // Collect into a mutable HashSet explicitly: SourceControlEventDAO.resetStaleEvents mutates
        // the set parameter (adds a sentinel id when empty), and Collectors.toSet() makes no
        // mutability guarantee. The empty-set short-circuit below makes that path unreachable, but
        // keep the mutable collector as defense-in-depth.
        .collect(Collectors.toCollection(HashSet::new)));

    if (activeInstanceIds.isEmpty()) {
      // Empty set means the heartbeat fetch returned nothing -- e.g. transient DB hiccup, fresh
      // cluster boot, or every heartbeat row simultaneously expired between refreshes. The DAO's
      // contract on an empty set is to substitute an invalid sentinel id, which would reset every
      // stale-looking event in the system, including events stamped by alive instances that just
      // haven't refreshed their heartbeat row yet. That blast radius is not acceptable; skip this
      // cycle and let the next 15s tick re-check.
      log.warn("No active SCM heartbeats found in perpetual_lock category '{}'; skipping stale-event reset",
          SourceControlLoadBalancer.LOAD_BALANCER_CATEGORY_FOR_SCM);
      return;
    }

    // The DAO call runs under the calling tenant's thread-local context, so in MTIQ it targets that
    // tenant's source_control_event table (a per-tenant table) rather than the global schema.
    sourceControlEventDAO.resetStaleEvents(activeInstanceIds, STALE_EVENT_CUTOFF_SECONDS);
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
