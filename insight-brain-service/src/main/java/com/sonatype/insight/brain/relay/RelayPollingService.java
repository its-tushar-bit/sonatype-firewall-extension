/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.git.PullRequestPollingScheduler;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import jakarta.ws.rs.NotAuthorizedException;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
import com.sonatype.insight.brain.model.githubapp.RelayLinkState;
import com.sonatype.insight.brain.model.relay.RelayConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.relay.dto.RelayAckResponse;
import com.sonatype.insight.brain.relay.dto.RelayEvent;
import com.sonatype.insight.brain.relay.dto.RelayEventsResponse;
import com.sonatype.insight.brain.security.OneTimeSystemRunnable;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import datadog.trace.api.Trace;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-tenant scheduler that polls the SCM webhook relay, maps events to
 * {@link SourceControlEvent}s, publishes them, and acknowledges drained events.
 *
 * <p>
 * While the relay is healthy the legacy {@link PullRequestPollingScheduler} is suppressed
 * to avoid double-delivering events. After {@value #DEFAULT_FAILURE_THRESHOLD} consecutive
 * relay failures the suppression is lifted so SCM polling can act as a fallback; the next
 * successful relay poll re-suppresses it.
 */
@Named
@Singleton
public class RelayPollingService
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(RelayPollingService.class);

  public static final int DEFAULT_POLL_INTERVAL_SECONDS = 60;

  public static final int DEFAULT_POLL_INITIAL_DELAY_SECONDS = 30;

  public static final int DEFAULT_MAX_EVENTS_PER_CYCLE = 50;

  public static final int DEFAULT_FAILURE_THRESHOLD = 3;

  /**
   * Per-cycle cap on relay-link retry attempts. Bounded so a backlog of {@link
   * RelayLinkState#ERROR} apps cannot starve event polling on a tenant with many Apps; rows
   * left over this cycle are picked up next tick.
   */
  static final int MAX_RELAY_LINK_RETRIES_PER_CYCLE = 10;

  private static final Set<String> RETRYABLE_LINK_STATES =
      Set.of(RelayLinkState.UNREGISTERED, RelayLinkState.ERROR);

  /** Hard cap on receipt handles per ack call; the relay rejects requests above this. */
  static final int ACK_BATCH_LIMIT = 100;

  /** Safety bound on the per-cycle drain loop in case the relay returns full pages forever. */
  static final int DEFAULT_MAX_DRAIN_ITERATIONS = 20;

  private static final int RELAY_POLLING_TENANT_THREAD_COUNT = 1;

  private final RelayClient relayClient;

  private final RelayRegistrationService relayRegistrationService;

  private final GitHubAppDAO gitHubAppDAO;

  private final GitHubAppRelayLinker gitHubAppRelayLinker;

  private final RelayEventMapper relayEventMapper;

  private final RelayEventDeduplicator relayEventDeduplicator;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final PullRequestPollingScheduler pullRequestPollingScheduler;

  private final PasswordHandler passwordHandler;

  private final ShutdownHandler shutdownHandler;

  private final ScmNodeProcessor scmNodeProcessor;

  private final RelayPollingStartDelayCalculator startDelayCalculator;

  private final TenantReference<ScheduledExecutorService> tenantScheduledExecutorServices;

  private final TenantReference<ScheduledFuture<?>> tenantPollingFuture;

  private final TenantReference<AtomicInteger> consecutiveFailures;

  // Set true while stopPolling() is tearing down so an in-flight pollOnce that completes
  // mid-teardown does not re-suppress legacy polling via recordSuccess/recordFailure.
  // Cleared at the end of stopPolling.
  private final TenantReference<AtomicBoolean> stopping;

  private final int pollIntervalSeconds;

  private final int pollInitialDelaySeconds;

  private final int maxEventsPerCycle;

  private final int failureThreshold;

  private final int maxDrainIterations;

  @VisibleForTesting
  boolean disableSchedulingForTesting;

  @Inject
  public RelayPollingService(
      RelayClient relayClient,
      RelayRegistrationService relayRegistrationService,
      GitHubAppDAO gitHubAppDAO,
      GitHubAppRelayLinker gitHubAppRelayLinker,
      RelayEventMapper relayEventMapper,
      RelayEventDeduplicator relayEventDeduplicator,
      SourceControlEventPublisher sourceControlEventPublisher,
      PullRequestPollingScheduler pullRequestPollingScheduler,
      PasswordHandler passwordHandler,
      ShutdownHandler shutdownHandler,
      ScmNodeProcessor scmNodeProcessor,
      RelayPollingStartDelayCalculator startDelayCalculator)
  {
    this(relayClient, relayRegistrationService, gitHubAppDAO, gitHubAppRelayLinker, relayEventMapper,
        relayEventDeduplicator, sourceControlEventPublisher, pullRequestPollingScheduler, passwordHandler,
        shutdownHandler, scmNodeProcessor, startDelayCalculator,
        DEFAULT_POLL_INITIAL_DELAY_SECONDS, DEFAULT_POLL_INTERVAL_SECONDS,
        DEFAULT_MAX_EVENTS_PER_CYCLE, DEFAULT_FAILURE_THRESHOLD, DEFAULT_MAX_DRAIN_ITERATIONS);
  }

  @VisibleForTesting
  RelayPollingService(
      RelayClient relayClient,
      RelayRegistrationService relayRegistrationService,
      GitHubAppDAO gitHubAppDAO,
      GitHubAppRelayLinker gitHubAppRelayLinker,
      RelayEventMapper relayEventMapper,
      RelayEventDeduplicator relayEventDeduplicator,
      SourceControlEventPublisher sourceControlEventPublisher,
      PullRequestPollingScheduler pullRequestPollingScheduler,
      PasswordHandler passwordHandler,
      ShutdownHandler shutdownHandler,
      ScmNodeProcessor scmNodeProcessor,
      RelayPollingStartDelayCalculator startDelayCalculator,
      int pollInitialDelaySeconds,
      int pollIntervalSeconds,
      int maxEventsPerCycle,
      int failureThreshold,
      int maxDrainIterations)
  {
    if (maxDrainIterations <= 0) {
      throw new IllegalArgumentException("maxDrainIterations must be > 0; got " + maxDrainIterations);
    }
    this.relayClient = relayClient;
    this.relayRegistrationService = relayRegistrationService;
    this.gitHubAppDAO = gitHubAppDAO;
    this.gitHubAppRelayLinker = gitHubAppRelayLinker;
    this.relayEventMapper = relayEventMapper;
    this.relayEventDeduplicator = relayEventDeduplicator;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.pullRequestPollingScheduler = pullRequestPollingScheduler;
    this.passwordHandler = passwordHandler;
    this.shutdownHandler = shutdownHandler;
    this.scmNodeProcessor = scmNodeProcessor;
    this.startDelayCalculator = startDelayCalculator;
    this.pollInitialDelaySeconds = pollInitialDelaySeconds;
    this.pollIntervalSeconds = pollIntervalSeconds;
    this.maxEventsPerCycle = maxEventsPerCycle;
    this.failureThreshold = failureThreshold;
    this.maxDrainIterations = maxDrainIterations;
    this.tenantScheduledExecutorServices = new TenantReference<>(this::newExecutor);
    this.tenantPollingFuture = new TenantReference<>(() -> null);
    this.consecutiveFailures = new TenantReference<>(AtomicInteger::new);
    this.stopping = new TenantReference<>(AtomicBoolean::new);
  }

  @Override
  public void register() {
    if (!scmNodeProcessor.shouldRun()) {
      return;
    }
    // Per-tenant: ensure this tenant is registered with the relay before polling starts.
    // registerOnStartup is idempotent and gated on the feature flag internally.
    //
    // Single-tenant note: RelayRegistrationService is itself a Dropwizard Managed singleton
    // and Dropwizard already invokes its start() (which calls registerOnStartup) at boot.
    // The call here is therefore redundant in single-tenant deployments — the second call
    // short-circuits on isRegistered() with a single DAO read. Keeping the call here keeps
    // the MTIQ per-tenant path symmetric (where registerOnStartup must run on each tenant
    // register and Dropwizard's per-process Managed hook does NOT).
    try {
      relayRegistrationService.registerOnStartup();
    }
    catch (RuntimeException e) {
      // Wording note: registerOnStartup itself runs once per tenant lifecycle; the "retry"
      // happens via the polling cycle's pre-flight branch — every pollOnce() with a null
      // relay_configuration calls registerOnDemand(). So polling continues to fire and
      // each cycle re-attempts registration until it succeeds (no admin action required
      // beyond the initial one that triggered registerOnStartup).
      log.warn("Tenant {} relay registration failed during register(); the polling cycle's pre-flight "
          + "will re-attempt registration on every cycle until success: {}",
          tenantSlug(), e.getMessage());
    }
    // A startup-time failure here (e.g. a transient DB hiccup in the per-tenant start-delay
    // calculator) would otherwise leave this tenant without relay polling for the JVM lifetime.
    try {
      startPolling();
      // INFO log per tenant on successful registration: a silent register() makes a
      // missing executor invisible to operators tailing the log. Tagging with the tenant
      // slug means "how many tenants actually scheduled polling?" is a single grep instead
      // of a jstack of RelayPolling-* threads.
      log.info("Relay polling registered for tenant {}", tenantSlug());
    }
    catch (RuntimeException e) {
      // register() runs once per tenant lifecycle; recovery requires a server restart or
      // explicit re-register call.
      log.warn("Tenant {} relay polling could not start; this tenant will not poll until the next process restart: {}",
          tenantSlug(), e.getMessage());
    }
  }

  /**
   * Returns the current tenant's slug for log tagging, or {@code "<unknown>"} if no tenant
   * context is bound on the calling thread. Single-tenant IQ has a fixed global tenant
   * slug; MTIQ binds the per-tenant slug before {@link TenantManaged#register()} runs.
   * Used purely for log discrimination — never feed back into business logic.
   */
  private String tenantSlug() {
    Tenant tenant = TenantThreadLocal.getTenant();
    return tenant == null ? "<unknown>" : tenant.tenantSlug;
  }

  @Override
  public void deregister() {
    if (scmNodeProcessor.shouldRun()) {
      // Stop local polling only. Relay-side deregistration (DELETE /api/register) is
      // intentionally scoped to hard-deletion via DeleteTenantsJob: a tenant that is
      // deprovisioned (this hook fires) without being deleted may be re-provisioned, in
      // which case the relay's customer record + SQS queue are still useful and would
      // need to be re-created if dropped here. Hard-deletion is the only path that
      // permanently removes the tenant; that path explicitly calls deregisterTenant().
      stopPolling();
    }
    else {
      // shouldRun() is false on this node — stopPolling() never ran, so nothing reset the
      // tenant's in-memory legacy-suppression flag. Restore it explicitly so a future
      // re-register on a node where shouldRun() is true does not start with stale
      // suppression that would silently disable legacy SCM polling.
      pullRequestPollingScheduler.setSuppressed(false);
    }
  }

  @VisibleForTesting
  ScheduledExecutorService newExecutor() {
    // Tag the thread with the current tenant slug. Each tenant gets its own executor (one
    // per TenantReference key) and each executor gets its own ThreadFactoryBuilder, so the
    // %d numbering would otherwise restart at 0 for every tenant — making jstack output
    // unable to distinguish per-tenant threads. Including the slug means a missing
    // executor (the failure mode of finding #3) is one grep away: 'jstack | grep RelayPolling'
    // shows one entry per registered tenant, not N entries all named 'RelayPolling-0'.
    String slug = tenantSlug();
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("RelayPolling-" + slug + "-%d").setDaemon(true).build();
    TenantScheduledThreadPoolExecutor executor =
        new TenantScheduledThreadPoolExecutor(RELAY_POLLING_TENANT_THREAD_COUNT, threadFactory);
    shutdownHandler.add(executor);
    return executor;
  }

  private void startPolling() {
    if (disableSchedulingForTesting) {
      return;
    }
    Runnable task = this::runPollCycleWithErrorBoundary;
    int initialDelay = startDelayCalculator.computeInitialDelaySeconds(pollIntervalSeconds, pollInitialDelaySeconds);
    ScheduledFuture<?> future = tenantScheduledExecutorServices.get()
        .scheduleAtFixedRate(task, initialDelay, pollIntervalSeconds, TimeUnit.SECONDS);
    tenantPollingFuture.set(future);
    // Tag with tenant slug + the calculator-derived initialDelay (mtiq) instead of the
    // raw pollInitialDelaySeconds default — staggering only matters when you can see what
    // each tenant landed on.
    log.info("Tenant {} scheduled relay polling every {}s starting in {}s",
        tenantSlug(), pollIntervalSeconds, initialDelay);
  }

  private void stopPolling() {
    // Mark stopping FIRST so an in-flight pollOnce's recordSuccess/recordFailure becomes
    // a no-op and does not re-suppress legacy polling after we restore it.
    stopping.get().set(true);
    try {
      // Restore legacy SCM polling so that any tenant left in the "suppressed" state at
      // deregistration time resumes discovery immediately on re-register. Always runs
      // even in test mode where executor cleanup is skipped.
      pullRequestPollingScheduler.setSuppressed(false);
      if (disableSchedulingForTesting) {
        // Tests construct a fresh service per case, so no per-tenant state bleeds across
        // tests. The executor/future/counter cleanup below is intentionally skipped here
        // because the test mode never created any of those resources.
        return;
      }
      ScheduledFuture<?> future = tenantPollingFuture.get();
      if (future != null) {
        future.cancel(false);
      }
      tenantPollingFuture.remove();
      ScheduledExecutorService executor = tenantScheduledExecutorServices.get();
      if (executor != null) {
        executor.shutdown();
      }
      tenantScheduledExecutorServices.remove();
      consecutiveFailures.remove();
      log.info("Stopped relay polling");
    }
    finally {
      stopping.remove();
    }
  }

  /**
   * Wraps {@link #pollOnce()} in a Throwable-catching boundary so a single uncaught
   * throwable cannot permanently cancel the recurring schedule.
   *
   * <p>
   * CRITICAL: must catch {@link Throwable}, not {@link RuntimeException}.
   * {@link java.util.concurrent.ScheduledThreadPoolExecutor#scheduleAtFixedRate}'s contract
   * is that ANY uncaught throwable from a scheduled task PERMANENTLY cancels the recurring
   * schedule ("if any execution of the task encounters an exception, subsequent executions
   * are suppressed"). With a RuntimeException-only catch, an {@link Error} (OOM,
   * LinkageError, NoSuchMethodError from a build-version mismatch, AOP instrumentation
   * surprise, AssertionError from a downstream library, etc.) would escape, the future
   * would die silently, and polling would stop for the JVM lifetime — no log line, no
   * counter, no fallback engagement, invisible until support tools.
   *
   * <p>
   * On any throwable: the ERROR log carries the real cause, and {@link #recordFailure()}
   * lets legacy SCM polling re-engage after the failure threshold so users keep getting PR
   * scans while the underlying issue is diagnosed. The pollErrors counter is incremented
   * in the observability branch where the counters bean is wired in (downstream of this
   * branch).
   */
  @VisibleForTesting
  void runPollCycleWithErrorBoundary() {
    OneTimeSystemRunnable runnable = new OneTimeSystemRunnable(this::pollOnce);
    try {
      runnable.run();
    }
    catch (Throwable t) {
      log.error("Relay polling cycle threw unexpectedly: {}", t.getMessage(), t);
      recordFailure();
    }
  }

  /**
   * Drains one batch of relay events. Visible for tests so they can drive the cycle directly
   * without dealing with the scheduled executor. In production this is invoked through
   * {@link #runPollCycleWithErrorBoundary()} so any uncaught throwable is logged and
   * recorded as a poll failure rather than escaping to the executor.
   */
  @Trace
  @VisibleForTesting
  void pollOnce() {
    if (!relayRegistrationService.isFeatureGateOpen()) {
      // DEBUG, not TRACE: a tenant whose poll cycle silently no-ops every minute because
      // its feature gate is closed should be diagnosable from the standard MTIQ log level.
      log.debug("Tenant {} skipping poll: feature gate closed", tenantSlug());
      // Gate closed → relay does not run; legacy SCM polling must remain active. Unlike
      // recordSuccess() / recordFailure(), this branch is NOT guarded by stopping.get().get():
      // both stopPolling() and this path call setSuppressed(false), so concurrent execution
      // is idempotent and the asymmetry is intentional (the guard there exists to avoid
      // re-suppressing legacy polling after stopPolling has un-suppressed it; both effects
      // here would be "un-suppress legacy", which is the desired terminal state).
      pullRequestPollingScheduler.setSuppressed(false);
      return;
    }
    RelayConfiguration configuration = relayRegistrationService.getConfiguration();
    if (configuration == null) {
      // No relay_configuration row yet; this tenant has the feature on but hasn't completed
      // registration. The pre-flight registerOnDemand() below will create it; until then
      // every poll cycle no-ops. Log so a stuck-in-pre-flight tenant is visible without
      // having to attach a debugger.
      log.debug("Tenant {} skipping poll: no relay_configuration row, calling registerOnDemand",
          tenantSlug());
      // Pre-flight: feature flag is on but no row yet; trigger registration and defer to next tick.
      // Pre-flight registration success is intentionally NOT a relay-health signal — the
      // registration call is a one-time DB+HTTP write to the relay, not a poll. Resetting
      // consecutiveFailures here would mask a relay that is reachable for /api/register but
      // failing /api/events. Failures here use recordFailure() because they DO indicate the
      // relay is unreachable; success leaves the counter unchanged so the next real poll's
      // outcome drives the fallback decision.
      try {
        relayRegistrationService.registerOnDemand();
      }
      catch (NotAuthorizedException e) {
        // 401 here means the relay already has a customer record under this license
        // fingerprint and we have no proof-of-possession (no api key, no webhook token)
        // to recover it. Common cause: an operator deleted the relay_configuration row
        // locally while the relay-side customer record is intact. Polling will loop
        // forever in this state without an explicit recovery action.
        //
        // State-transition-only WARN: relay polling keeps firing every pollIntervalSeconds
        // even after fallback engages (ensureFallbackPolling only un-suppresses legacy
        // polling, it doesn't cancel the relay future), so an unconditional WARN would
        // emit ~1440 lines/day per stuck tenant. Gate on consecutiveFailures==0 so the
        // operator-actionable diagnosis is logged exactly once per stuck-state entry; if
        // a recovery succeeds and the state breaks again later, recordSuccess() will
        // have reset the counter and the next 401 will WARN again. Subsequent cycles
        // log at DEBUG so the loop is still observable at trace level without spam.
        AtomicInteger counter = consecutiveFailures.get();
        if (counter.get() == 0) {
          log.warn("Pre-flight relay registration rejected (HTTP 401) for tenant {}; the relay holds a "
              + "customer record for this license but IQ has no credential to recover it. Manual recovery required "
              + "(see docs/relay/operator-runbook.md, stuck state #1). Cause: {}",
              tenantSlug(), e.getMessage());
        }
        else {
          log.debug("Tenant {} pre-flight relay registration still rejected (HTTP 401); cycle {}; awaiting recovery",
              tenantSlug(), counter.get());
        }
        recordFailure();
      }
      catch (RuntimeException e) {
        log.warn("Pre-flight relay registration failed; deferring to next poll tick: {}", e.getMessage());
        recordFailure();
      }
      return;
    }
    // Pre-flight: retry GitHub App relay-registration for rows in UNREGISTERED or ERROR.
    // Bounded so a large backlog cannot starve event polling on a busy tenant. Failures here
    // are NOT recordFailure() — a per-App registration miss is a per-row health problem, not
    // a relay-wide outage signal, and we don't want a single bad install to flap the legacy
    // SCM polling fallback for the whole tenant.
    retryGitHubAppRelayLinks();
    String apiKey = decryptApiKey(configuration.getApiKey());
    if (apiKey == null) {
      // Distinguish absent vs. undecryptable so the WARN is specific. The decrypt path
      // already logged the underlying exception (or returned null without logging when
      // the column was empty), so this WARN doesn't repeat it.
      String reason = configuration.getApiKey() == null || configuration.getApiKey().isEmpty()
          ? "apiKey column is empty"
          : "apiKey ciphertext could not be decrypted";
      log.warn("Relay configuration present but unusable ({}); skipping poll cycle", reason);
      recordFailure();
      return;
    }
    // Mode is derived once per cycle from the local configuration row: PAT mode iff
    // webhook_url is populated, GitHub App mode iff blank. Threading it through to the
    // dedup writes/reads is what discriminates rows from a prior-mode registration after
    // a customer migrates (CLM-39685 follow-up).
    String mode = RelayMode.fromConfiguration(configuration);

    // Per-cycle caches: when several events share a repository URL (common for active repos)
    // the DAO lookups inside RelayEventMapper happen once per unique URL, not per event.
    Map<String, List<Application>> applicationsByRepoUrl = new HashMap<>();
    Map<String, List<SourceControl>> sourceControlsByRepoUrl = new HashMap<>();
    // PAT-mode iff the relay returned a per-customer webhook URL on registration; GitHub App
    // mode iff blank (registration is by App-level secret + installation routing). Threaded
    // into every produced SourceControlEvent so support-zip / telemetry can attribute relay
    // events to a mode without re-reading the configuration row.
    String authenticationType = StringUtils.isBlank(configuration.getWebhookUrl())
        ? SourceControl.AuthenticationType.GITHUB_APP.name()
        : SourceControl.AuthenticationType.PAT.name();
    int totalDrained = 0;
    int totalPublished = 0;
    // Drain loop: keep pulling pages while the relay returns a full batch (more events queued).
    // Bounded to avoid runaway if the relay misbehaves.
    boolean drained = false;
    for (int iteration = 0; iteration < maxDrainIterations; iteration++) {
      RelayEventsResponse response;
      try {
        response = relayClient.pollEvents(apiKey, maxEventsPerCycle);
      }
      catch (RuntimeException e) {
        log.warn("Relay poll failed: {}", e.getMessage());
        recordFailure();
        return;
      }
      List<RelayEvent> events = response.getEvents() != null ? response.getEvents() : List.of();
      if (events.isEmpty()) {
        // Queue empty on this iteration — fully drained (including the common idle case).
        drained = true;
        break;
      }
      List<String> handlesToAck = new ArrayList<>(events.size());
      int published = processEvents(events, handlesToAck, applicationsByRepoUrl, sourceControlsByRepoUrl,
          authenticationType, mode);
      totalPublished += published;
      totalDrained += events.size();
      if (!ackInBatches(apiKey, handlesToAck)) {
        return;
      }
      // Less than a full page → relay queue is drained.
      if (events.size() < maxEventsPerCycle) {
        drained = true;
        break;
      }
    }

    if (!drained) {
      // Hit the iteration cap with full pages still arriving — events likely remain in the
      // queue. The next scheduled tick will drain the remainder. Logged at INFO so it's
      // visible in operator-facing diagnostics without spamming on a healthy queue.
      log.info("Relay poll cycle hit maxDrainIterations={} with full pages; remainder will be drained next tick",
          maxDrainIterations);
    }
    log.debug("Relay poll cycle: drained {} event(s), published {} SourceControlEvent(s)", totalDrained,
        totalPublished);
    recordSuccess();
  }

  private void retryGitHubAppRelayLinks() {
    List<GitHubApp> retryable;
    try {
      retryable = gitHubAppDAO.getActiveByRelayLinkState(RETRYABLE_LINK_STATES);
    }
    catch (RuntimeException e) {
      // A DAO failure here is a local DB problem, not a relay outage; log and continue with
      // the actual event poll.
      log.warn("Failed to load GitHub Apps for relay-link retry: {}", e.getMessage());
      return;
    }
    if (retryable.isEmpty()) {
      return;
    }
    int budget = Math.min(retryable.size(), MAX_RELAY_LINK_RETRIES_PER_CYCLE);
    log.debug("Retrying relay-link registration for {} GitHub App(s) (of {} eligible)", budget, retryable.size());
    for (int i = 0; i < budget; i++) {
      GitHubApp app = retryable.get(i);
      try {
        gitHubAppRelayLinker.tryRegister(app);
      }
      catch (RuntimeException e) {
        // tryRegister is supposed to be exception-safe; log defensively and keep going.
        log.warn("Unexpected error retrying relay link for GitHub App {}: {}",
            app.getId(), e.getMessage());
      }
    }
  }

  private int processEvents(
      List<RelayEvent> events,
      List<String> handlesToAck,
      Map<String, List<Application>> applicationsByRepoUrl,
      Map<String, List<SourceControl>> sourceControlsByRepoUrl,
      String authenticationType,
      String mode)
  {
    int published = 0;
    for (RelayEvent event : events) {
      if (event == null) {
        continue;
      }
      if (event.getReceiptHandle() != null) {
        // Ack regardless of dedup/mapper/publisher outcome — acking the receipt handle
        // prevents the relay from redelivering the same payload after the SQS visibility
        // timeout. For duplicates, unmappable events, and downstream processing failures
        // we'd rather drop the event than redeliver it indefinitely; idempotent retry on
        // the IQ side is not feasible without per-event durability we don't have.
        // Documented as intentional in the class-level Javadoc.
        handlesToAck.add(event.getReceiptHandle());
      }
      String eventId = event.getEventId();
      if (relayEventDeduplicator.isPrimaryDuplicate(eventId)) {
        log.debug("Skipping duplicate relay eventId={}", eventId);
        continue;
      }
      try {
        List<SourceControlEvent> mapped =
            relayEventMapper.map(event, applicationsByRepoUrl, sourceControlsByRepoUrl, authenticationType);
        // Capture the secondary key from the first app whose check actually passed (preferred),
        // OR — if every app is a secondary-dup — from the first dup'd app as a fallback so
        // the relay_event_log row reflects the (app, pr, commit) we deduped on. Without the
        // dup fallback, all-secondary-dup events recorded a half-row (null app/pr/commit)
        // that operators reading the table could not distinguish from a mapper failure.
        // Recording the dup'd tuple does not affect dedup correctness: the row whose
        // existence we just confirmed via isSecondaryDuplicate already carries that tuple,
        // and the secondary index is non-unique so a duplicate-tuple row is harmless.
        String firstAppPublicId = null;
        Integer firstPrNumber = null;
        String firstCommitHash = null;
        String firstDupAppPublicId = null;
        Integer firstDupPrNumber = null;
        String firstDupCommitHash = null;
        for (SourceControlEvent sourceControlEvent : mapped) {
          String appPublicId = relayEventDeduplicator.resolveApplicationPublicId(
              sourceControlEvent.getApplicationId());
          if (relayEventDeduplicator.isSecondaryDuplicate(appPublicId,
              sourceControlEvent.getPullRequestNumber(), sourceControlEvent.getCommitHash(), mode,
              event.getEventType()))
          {
            if (firstDupAppPublicId == null) {
              firstDupAppPublicId = appPublicId;
              firstDupPrNumber = sourceControlEvent.getPullRequestNumber();
              firstDupCommitHash = sourceControlEvent.getCommitHash();
            }
            log.debug("Skipping secondary-duplicate relay eventId={} app={} pr={} commit={} type={}",
                eventId, appPublicId, sourceControlEvent.getPullRequestNumber(),
                sourceControlEvent.getCommitHash(), event.getEventType());
            continue;
          }
          sourceControlEventPublisher.publishEventBypassingFeatureGate(sourceControlEvent);
          if (firstAppPublicId == null) {
            // Capture the secondary key AFTER successful publish so a publish failure on
            // the first non-dup app doesn't leave us with a captured-but-not-actually-
            // recorded tuple (the outer catch would still record null fields anyway, but
            // capturing only after success keeps the intent clean).
            firstAppPublicId = appPublicId;
            firstPrNumber = sourceControlEvent.getPullRequestNumber();
            firstCommitHash = sourceControlEvent.getCommitHash();
          }
          published++;
        }
        // Prefer the non-dup tuple; fall back to the dup tuple if every app was a dup;
        // null only when mapped.isEmpty() (no app bound to the repository URL).
        //
        // recordProcessed is wrapped in its own try so a dedup-log DB failure here is
        // logged accurately as a dedup-write failure (NOT misclassified as a
        // processing failure) and does NOT fall through to the outer catch block's
        // null-secondary-key fallback (which would overwrite a successful publish).
        String recordApp = firstAppPublicId != null ? firstAppPublicId : firstDupAppPublicId;
        Integer recordPr = firstAppPublicId != null ? firstPrNumber : firstDupPrNumber;
        String recordCommit = firstAppPublicId != null ? firstCommitHash : firstDupCommitHash;
        try {
          relayEventDeduplicator.recordProcessed(eventId, recordApp, recordPr, recordCommit,
              event.getEventType(), mode);
        }
        catch (RuntimeException recordError) {
          log.warn("Failed to record relay eventId={} in dedup log after successful processing: {}",
              eventId, recordError.getMessage());
        }
      }
      catch (RuntimeException e) {
        log.error("Failed to process relay eventId={}: {}", eventId, e.getMessage(), e);
        // Receipt handle is already in handlesToAck (added before the try). Record the
        // event id in the dedup log even on failure so that a redelivery via the legacy
        // path (with a different UUID) is still recognized by primary dedup.
        try {
          relayEventDeduplicator.recordProcessed(eventId, null, null, null, event.getEventType(), mode);
        }
        catch (RuntimeException recordError) {
          log.warn("Failed to record relay eventId={} in dedup log after processing error: {}",
              eventId, recordError.getMessage());
        }
      }
    }
    return published;
  }

  /**
   * Acks {@code handlesToAck} in chunks no larger than {@link #ACK_BATCH_LIMIT} so we never
   * exceed the relay's per-call cap. Returns {@code false} (and records a failure) when
   * any chunk's ack call throws; the caller should then abort the cycle so unacked events
   * get redelivered after their visibility timeout.
   */
  private boolean ackInBatches(String apiKey, List<String> handlesToAck) {
    for (int from = 0; from < handlesToAck.size(); from += ACK_BATCH_LIMIT) {
      int to = Math.min(from + ACK_BATCH_LIMIT, handlesToAck.size());
      List<String> chunk = handlesToAck.subList(from, to);
      try {
        RelayAckResponse ack = relayClient.ack(apiKey, chunk);
        if (!ack.getFailed().isEmpty()) {
          log.warn("Relay ack reported {} failed handle(s); they will be redelivered", ack.getFailed().size());
        }
      }
      catch (RuntimeException e) {
        // Don't crash the poller on ack failures; events will be redelivered after their
        // visibility timeout. This counts as a failure for fallback purposes.
        log.warn("Relay ack failed for {} handle(s): {}", chunk.size(), e.getMessage());
        recordFailure();
        return false;
      }
    }
    return true;
  }

  private void recordSuccess() {
    if (stopping.get().get()) {
      // stopPolling has already restored legacy suppression; don't undo that.
      return;
    }
    AtomicInteger counter = consecutiveFailures.get();
    int prior = counter.get();
    if (prior > 0) {
      // Only mention the fallback if it was actually engaged (failures crossed the threshold).
      // For sub-threshold blips, ensureFallbackPolling(false) below is a no-op anyway.
      if (prior >= failureThreshold) {
        log.info("Relay polling recovered after {} consecutive failure(s); disabling SCM polling fallback", prior);
      }
      else {
        log.info("Relay polling recovered after {} consecutive failure(s)", prior);
      }
      counter.set(0);
    }
    // Always re-assert suppression on success: covers the cold-start case where the
    // first poll succeeds without any prior failure to recover from.
    ensureFallbackPolling(false);
  }

  private void recordFailure() {
    if (stopping.get().get()) {
      return;
    }
    int failures = consecutiveFailures.get().incrementAndGet();
    if (failures >= failureThreshold) {
      ensureFallbackPolling(true);
    }
  }

  /**
   * Drives suppression of the legacy SCM polling scheduler. When the relay is healthy we
   * suppress legacy polling; when degraded we lift the suppression so it can fill in.
   */
  private void ensureFallbackPolling(boolean fallbackActive) {
    boolean shouldSuppressLegacy = !fallbackActive;
    if (pullRequestPollingScheduler.setSuppressed(shouldSuppressLegacy) && fallbackActive) {
      log.warn("Relay polling failed {} consecutive times; activating SCM polling fallback",
          consecutiveFailures.get().get());
    }
  }

  private String decryptApiKey(String encryptedApiKey) {
    if (encryptedApiKey == null || encryptedApiKey.isEmpty()) {
      return null;
    }
    try {
      return passwordHandler.decryptPassword(encryptedApiKey);
    }
    catch (RuntimeException e) {
      // DEBUG only: the caller's WARN at the use site distinguishes empty-vs-undecryptable
      // and is the operator-facing log line. Logging at ERROR here too would produce
      // duplicate entries for one root cause.
      log.debug("Relay api key decryption failed: {}", e.getMessage());
      return null;
    }
  }
}
