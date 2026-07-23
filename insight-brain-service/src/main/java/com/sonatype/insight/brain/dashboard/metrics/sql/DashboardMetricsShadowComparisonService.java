/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsDTO;
import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsRequestDTO;
import com.sonatype.insight.brain.dashboard.metrics.MetricValueDTO;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlTelemetry.ShadowOutcome;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Samples completed index responses and compares their migrated values to SQL off the request thread.
 */
@Named
@Singleton
public class DashboardMetricsShadowComparisonService
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(DashboardMetricsShadowComparisonService.class);

  private static final int RAW_MISMATCH_MAXIMUM_SIZE = 10_000;

  private static final Duration RAW_MISMATCH_TTL = Duration.ofHours(24);

  private final DashboardMetricsScopeResolver resolver;

  private final DashboardMetricsSqlCoordinator coordinator;

  private final DashboardMetricsSqlModeProvider modeProvider;

  private final DashboardMetricsSqlTelemetry telemetry;

  private final IntPredicate sampler;

  @Nullable
  private final Function<Runnable, ?> taskSubmitter;

  private final Supplier<Subject> subjectSupplier;

  @Nullable
  private final Cache<ComparisonKey, RawObservation> fixedRawMismatches;

  @Nullable
  private final TenantReference<TenantResources> resourcesByTenant;

  @Nullable
  private final ShutdownHandler shutdownHandler;

  @Inject
  public DashboardMetricsShadowComparisonService(
      final DashboardMetricsScopeResolver resolver,
      final DashboardMetricsSqlCoordinator coordinator,
      final DashboardMetricsSqlModeProvider modeProvider,
      final DashboardMetricsSqlTelemetry telemetry,
      final ShutdownHandler shutdownHandler)
  {
    this(resolver, coordinator, modeProvider, telemetry, shutdownHandler, SecurityUtils::getSubject);
  }

  DashboardMetricsShadowComparisonService(
      final DashboardMetricsScopeResolver resolver,
      final DashboardMetricsSqlCoordinator coordinator,
      final DashboardMetricsSqlModeProvider modeProvider,
      final DashboardMetricsSqlTelemetry telemetry,
      final ShutdownHandler shutdownHandler,
      final Supplier<Subject> subjectSupplier)
  {
    this.resolver = resolver;
    this.coordinator = coordinator;
    this.modeProvider = modeProvider;
    this.telemetry = telemetry;
    this.sampler = denominator -> ThreadLocalRandom.current().nextInt(denominator) == 0;
    this.subjectSupplier = subjectSupplier;
    this.shutdownHandler = shutdownHandler;
    this.taskSubmitter = null;
    this.fixedRawMismatches = null;

    this.resourcesByTenant = new TenantReference<>(() -> {
      TenantThreadPoolExecutor executor = new TenantThreadPoolExecutor(
          1,
          2,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(256),
          new ThreadFactoryBuilder()
              .setDaemon(true)
              .setNameFormat("dashboard-metrics-shadow-%d")
              .build(),
          new ThreadPoolExecutor.AbortPolicy(),
          "dashboard-metrics-shadow",
          "DashboardMetricsShadowComparisonService");
      executor.allowCoreThreadTimeOut(true);
      return new TenantResources(executor, newRawMismatchCache(), new AtomicBoolean());
    });
  }

  DashboardMetricsShadowComparisonService(
      final DashboardMetricsScopeResolver resolver,
      final DashboardMetricsSqlCoordinator coordinator,
      final DashboardMetricsSqlModeProvider modeProvider,
      final DashboardMetricsSqlTelemetry telemetry,
      final IntPredicate sampler,
      final Executor executor,
      final Supplier<Subject> subjectSupplier)
  {
    this.resolver = resolver;
    this.coordinator = coordinator;
    this.modeProvider = modeProvider;
    this.telemetry = telemetry;
    this.sampler = sampler;
    this.taskSubmitter = task -> AuditData.get().continueAsync(task, runnable -> {
      executor.execute(runnable);
      return null;
    });
    this.subjectSupplier = subjectSupplier;
    this.fixedRawMismatches = newRawMismatchCache();
    this.resourcesByTenant = null;
    this.shutdownHandler = null;
  }

  public void maybeSchedule(
      final DashboardMetricsRequestDTO request,
      final DashboardMetricsDTO servedIndexResponse,
      final Instant requestInstant,
      @Nullable final Long indexSnapshotTime)
  {
    try {
      OptionalInt denominator = modeProvider.shadowSampleDenominator();
      if (denominator.isEmpty() || !sampler.test(denominator.getAsInt())) {
        return;
      }

      DashboardMetricsShadowCapture capture =
          DashboardMetricsShadowCapture.create(request, requestInstant, indexSnapshotTime, servedIndexResponse);
      Subject subject = subjectSupplier.get();
      if (resourcesByTenant != null) {
        TenantResources resources = resourcesByTenant.get();
        boolean submitted;
        synchronized (resources.lifecycleLock()) {
          if (resources.closed().get()) {
            submitted = false;
          }
          else {
            if (resources.shutdownRegistered().compareAndSet(false, true)) {
              shutdownHandler.add(resources.executor());
            }
            Runnable task = subject.associateWith(() -> compare(capture, resources.rawMismatches()));
            AuditData.get().continueAsync(task, resources.executor()::submit);
            submitted = true;
          }
        }
        if (submitted) {
          recordShadowOutcome(ShadowOutcome.SAMPLED);
        }
        else {
          log.warn("Dashboard SQL shadow comparison dropped because tenant resources are closed");
          recordShadowOutcome(ShadowOutcome.DROPPED);
        }
      }
      else {
        Runnable task = subject.associateWith(() -> compare(capture, fixedRawMismatches));
        taskSubmitter.apply(task);
        recordShadowOutcome(ShadowOutcome.SAMPLED);
      }
    }
    catch (RejectedExecutionException e) {
      log.warn("Dashboard SQL shadow comparison dropped", e);
      recordShadowOutcome(ShadowOutcome.DROPPED);
    }
    catch (RuntimeException e) {
      log.warn("Dashboard SQL shadow comparison failed to schedule", e);
      recordShadowOutcome(ShadowOutcome.FAILED);
    }
  }

  @Override
  public void deregister() {
    if (resourcesByTenant == null) {
      return;
    }
    TenantResources resources = resourcesByTenant.remove();
    if (resources == null) {
      return;
    }
    synchronized (resources.lifecycleLock()) {
      resources.closed().set(true);
      shutdownHandler.remove(resources.executor());
      resources.executor().shutdownNow();
      resources.rawMismatches().invalidateAll();
    }
  }

  private void compare(
      final DashboardMetricsShadowCapture capture,
      final Cache<ComparisonKey, RawObservation> rawMismatches)
  {
    try {
      List<MetricPair> present = presentIndexMetrics(capture.servedIndexResponse());
      int skipped = 0;
      boolean anyEligible = false;
      for (MetricPair pair : present) {
        if (eligible(pair.indexValue())) {
          anyEligible = true;
        }
        else {
          skipped++;
        }
      }
      if (!anyEligible) {
        log.debug("Dashboard SQL shadow skipped filterHash={} tier={} skipped={}",
            capture.filterHash(), tier(capture.includeHeavyMetrics()), skipped);
        recordShadowOutcome(ShadowOutcome.COMPLETED);
        return;
      }

      ResolvedScope scope = resolver.resolve(capture.toRequest());
      if (!scope.isQueryable()) {
        log.debug("Dashboard SQL shadow skipped non-queryable scope filterHash={} denyReason={}",
            capture.filterHash(), scope.denyReason());
        recordShadowOutcome(ShadowOutcome.COMPLETED);
        return;
      }

      String principalShape = principalShape(scope);
      String scopeHash = scopeHash(scope);
      boolean metricFailed = false;
      for (MetricPair pair : present) {
        if (!eligible(pair.indexValue())) {
          continue;
        }
        try {
          MetricValueDTO sqlValue = sqlValue(pair.metric(), scope);
          if (!eligible(sqlValue)) {
            skipped++;
            continue;
          }
          classify(capture, scopeHash, pair.metric(), pair.indexValue(), sqlValue, rawMismatches);
        }
        catch (RuntimeException e) {
          skipped++;
          metricFailed = true;
          log.warn("Dashboard SQL shadow metric comparison failed metric={} filterHash={}",
              pair.metric(), capture.filterHash(), e);
        }
      }
      log.debug("Dashboard SQL shadow completed filterHash={} principalShape={} tier={} skipped={}",
          capture.filterHash(), principalShape, tier(capture.includeHeavyMetrics()), skipped);
      recordShadowOutcome(metricFailed ? ShadowOutcome.FAILED : ShadowOutcome.COMPLETED);
    }
    catch (RuntimeException e) {
      log.warn("Dashboard SQL shadow comparison failed", e);
      recordShadowOutcome(ShadowOutcome.FAILED);
    }
  }

  private void classify(
      final DashboardMetricsShadowCapture capture,
      final String scopeHash,
      final Metric metric,
      final MetricValueDTO indexValue,
      final MetricValueDTO sqlValue,
      final Cache<ComparisonKey, RawObservation> rawMismatches)
  {
    ComparisonKey key = new ComparisonKey(
        capture.filterHash(), scopeHash, tier(capture.includeHeavyMetrics()), metric);
    boolean valuesEqual =
        Objects.equals(indexValue.total, sqlValue.total)
            && Objects.equals(indexValue.breakdown, sqlValue.breakdown);
    if (valuesEqual) {
      rawMismatches.invalidate(key);
      return;
    }

    Long snapshotTime = capture.indexSnapshotTime();
    if (snapshotTime == null) {
      recordMismatch(false);
      return;
    }

    RawObservation first =
        rawMismatches.asMap().putIfAbsent(key, new RawObservation(snapshotTime));
    if (first == null) {
      recordMismatch(false);
      return;
    }
    recordMismatch(snapshotTime > first.indexSnapshotTime());
  }

  private MetricValueDTO sqlValue(final Metric metric, final ResolvedScope scope) {
    return switch (metric) {
      case APPLICATIONS -> coordinator.countApplications(scope);
      case ORGANIZATIONS -> coordinator.countOrganizations(scope);
      case POLICIES -> coordinator.countPolicies(scope);
      case VIOLATIONS -> coordinator.countViolations(scope);
    };
  }

  private static List<MetricPair> presentIndexMetrics(final DashboardMetricsDTO response) {
    List<MetricPair> metrics = new ArrayList<>();
    addIfPresent(metrics, Metric.APPLICATIONS, response.applications);
    addIfPresent(metrics, Metric.ORGANIZATIONS, response.organizations);
    addIfPresent(metrics, Metric.POLICIES, response.policies);
    addIfPresent(metrics, Metric.VIOLATIONS, response.violations);
    return metrics;
  }

  private static void addIfPresent(
      final List<MetricPair> metrics,
      final Metric metric,
      @Nullable final MetricValueDTO value)
  {
    if (value != null) {
      metrics.add(new MetricPair(metric, value));
    }
  }

  private static boolean eligible(@Nullable final MetricValueDTO value) {
    return value != null && value.total != null && value.errorCode == null;
  }

  static String principalShape(final ResolvedScope scope) {
    return scope.kind().name() + ':' + ownerBucket(scope.ownerIds().size());
  }

  static String scopeHash(final ResolvedScope scope) {
    String normalized = scope.kind().name()
        + '|' + Objects.toString(scope.denyReason(), "")
        + '|' + DashboardMetricsSqlHashes.normalizeIds(scope.ownerIds())
        + '|' + DashboardMetricsSqlHashes.normalizeIds(scope.policyOwnerIds())
        + '|' + DashboardMetricsSqlHashes.normalizeIds(scope.organizationIds())
        + '|' + DashboardMetricsSqlHashes.normalizeIds(scope.applicationIds());
    return DashboardMetricsSqlHashes.sha256Hex(normalized);
  }

  private static String ownerBucket(final int size) {
    if (size == 0) {
      return "OWNERS_0";
    }
    if (size == 1) {
      return "OWNERS_1";
    }
    if (size <= 10) {
      return "OWNERS_2_10";
    }
    if (size <= 100) {
      return "OWNERS_11_100";
    }
    return "OWNERS_101_PLUS";
  }

  private static String tier(@Nullable final Boolean includeHeavyMetrics) {
    if (includeHeavyMetrics == null) {
      return "COMPATIBILITY";
    }
    return includeHeavyMetrics ? "HEAVY" : "SUMMARY";
  }

  private void recordShadowOutcome(final ShadowOutcome outcome) {
    try {
      telemetry.recordShadowOutcome(outcome);
    }
    catch (RuntimeException ignored) {
      // Telemetry is strictly best-effort.
    }
  }

  private void recordMismatch(final boolean persistent) {
    try {
      telemetry.recordMismatch(persistent);
    }
    catch (RuntimeException ignored) {
      // Telemetry is strictly best-effort.
    }
  }

  private static Cache<ComparisonKey, RawObservation> newRawMismatchCache() {
    return CacheBuilder.newBuilder()
        .maximumSize(RAW_MISMATCH_MAXIMUM_SIZE)
        .expireAfterWrite(RAW_MISMATCH_TTL)
        .build();
  }

  private enum Metric
  {
    APPLICATIONS,
    ORGANIZATIONS,
    POLICIES,
    VIOLATIONS
  }

  private record MetricPair(Metric metric, MetricValueDTO indexValue)
  {
  }

  private record ComparisonKey(String filterHash, String scopeHash, String tier, Metric metric)
  {
  }

  private record RawObservation(long indexSnapshotTime)
  {
  }

  private record TenantResources(
      TenantThreadPoolExecutor executor,
      Cache<ComparisonKey, RawObservation> rawMismatches,
      AtomicBoolean shutdownRegistered,
      AtomicBoolean closed,
      Object lifecycleLock)
  {
    private TenantResources(
        final TenantThreadPoolExecutor executor,
        final Cache<ComparisonKey, RawObservation> rawMismatches,
        final AtomicBoolean shutdownRegistered)
    {
      this(executor, rawMismatches, shutdownRegistered, new AtomicBoolean(), new Object());
    }
  }
}
