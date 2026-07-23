/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsDTO;
import com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsRequestDTO;
import com.sonatype.insight.brain.dashboard.metrics.MetricValueDTO;
import com.sonatype.insight.brain.dashboard.metrics.sql.DashboardMetricsSqlTelemetry.ShadowOutcome;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.test.LogOutput;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class DashboardMetricsShadowComparisonServiceTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(DashboardMetricsShadowComparisonService.class);

  private final DashboardMetricsScopeResolver resolver = mock(DashboardMetricsScopeResolver.class);

  private final DashboardMetricsSqlCoordinator coordinator = mock(DashboardMetricsSqlCoordinator.class);

  private final DashboardMetricsSqlModeProvider modeProvider = mock(DashboardMetricsSqlModeProvider.class);

  private final DashboardMetricsSqlTelemetry telemetry = mock(DashboardMetricsSqlTelemetry.class);

  private final ResolvedScope scope = new ResolvedScope(
      ResolvedScope.Kind.RESTRICTED,
      null,
      Set.of("owner"),
      Set.of("policy-owner"),
      Set.of("org"),
      Set.of("app"),
      false);

  @After
  public void tearDown() {
    ThreadContext.remove();
    TenantTestHelper.resetAfterTest();
  }

  @Test
  public void defaultDenominatorSamplesOneInTwentyUsingInjectedSampler() {
    List<Integer> denominators = new ArrayList<>();
    DashboardMetricsShadowComparisonService service =
        service(denominator -> {
          denominators.add(denominator);
          return true;
        }, Runnable::run);
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(20));
    stubMatchingSql();

    service.maybeSchedule(request(), indexResponse(1L, 1000L), Instant.EPOCH, 1000L);

    assertThat(denominators).containsExactly(20);
    verify(telemetry).recordShadowOutcome(ShadowOutcome.SAMPLED);
  }

  @Test
  public void disabledSamplingNeverSubmits() {
    AtomicBoolean submitted = new AtomicBoolean();
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.empty());

    service(ignored -> true, task -> submitted.set(true))
        .maybeSchedule(request(), indexResponse(1L, 1000L), Instant.EPOCH, 1000L);

    assertThat(submitted).isFalse();
    verify(resolver, never()).resolve(any());
  }

  @Test
  public void shadowReturnsServedIndexDtoBeforeWorkerCompletes() {
    List<Runnable> submitted = new ArrayList<>();
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    DashboardMetricsShadowComparisonService service = service(ignored -> true, submitted::add);
    DashboardMetricsDTO served = indexResponse(7L, 1000L);

    service.maybeSchedule(request(), served, Instant.EPOCH, 1000L);

    assertThat(submitted).hasSize(1);
    verify(resolver, never()).resolve(any());
    submitted.get(0).run();
    verify(resolver).resolve(any());
  }

  @Test
  public void queueRejectionDropsComparisonWithoutFailingRequest() {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    Executor rejecting = task -> {
      throw new RejectedExecutionException("full");
    };

    service(ignored -> true, rejecting)
        .maybeSchedule(request(), indexResponse(1L, 1000L), Instant.EPOCH, 1000L);

    verify(telemetry).recordShadowOutcome(ShadowOutcome.DROPPED);
    verify(telemetry, never()).recordShadowOutcome(ShadowOutcome.SAMPLED);
    verify(resolver, never()).resolve(any());
  }

  @Test
  public void workerRunsWithCapturedTenantAndShiroSubject() {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    Subject captured = new Subject.Builder(mock(org.apache.shiro.mgt.SecurityManager.class)).buildSubject();
    AtomicBoolean subjectObserved = new AtomicBoolean();
    when(resolver.resolve(any())).thenAnswer(invocation -> {
      subjectObserved.set(ThreadContext.getSubject() == captured);
      return scope;
    });
    stubMatchingSql();

    service(ignored -> true, Runnable::run, () -> captured)
        .maybeSchedule(request(), indexResponse(1L, 1000L), Instant.EPOCH, 1000L);

    assertThat(subjectObserved).isTrue();
  }

  @Test
  public void productionExecutorPropagatesTenantContext() throws Exception {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    Subject captured = mock(Subject.class);
    when(captured.associateWith(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ShutdownHandler shutdownHandler = mock(ShutdownHandler.class);
    CountDownLatch workerRan = new CountDownLatch(1);
    AtomicReference<Tenant> expectedTenant = new AtomicReference<>();
    AtomicReference<Throwable> tenantFailure = new AtomicReference<>();
    when(resolver.resolve(any())).thenAnswer(invocation -> {
      try {
        TenantTestHelper.assertTenantSet(expectedTenant.get());
      }
      catch (Throwable failure) {
        tenantFailure.set(failure);
      }
      finally {
        workerRan.countDown();
      }
      return scope;
    });
    when(coordinator.countApplications(scope)).thenReturn(new MetricValueDTO(1L, null, "sql"));
    DashboardMetricsShadowComparisonService service = new DashboardMetricsShadowComparisonService(
        resolver, coordinator, modeProvider, telemetry, shutdownHandler, () -> captured);

    TenantTestHelper.initMultiTenantMode();
    TenantTestHelper.testAsNewTenant("dashboard-shadow-context", tenant -> {
      expectedTenant.set(tenant);
      service.maybeSchedule(request(), indexResponse(1L, 1000L), Instant.EPOCH, 1000L);
      verify(telemetry).recordShadowOutcome(ShadowOutcome.SAMPLED);
      verify(telemetry, never()).recordShadowOutcome(ShadowOutcome.FAILED);
      verify(shutdownHandler).add(any(TenantThreadPoolExecutor.class));
      assertThat(workerRan.await(5, TimeUnit.SECONDS)).isTrue();
    });

    assertThat(tenantFailure.get()).isNull();
    ArgumentCaptor<TenantThreadPoolExecutor> executor =
        ArgumentCaptor.forClass(TenantThreadPoolExecutor.class);
    verify(shutdownHandler).add(executor.capture());
    executor.getValue().shutdownNow();
  }

  @Test
  public void deregisterRemovesAndShutsDownOnlyCurrentTenantResourcesIdempotently() {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    Subject captured = mock(Subject.class);
    when(captured.associateWith(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ShutdownHandler shutdownHandler = mock(ShutdownHandler.class);
    DashboardMetricsShadowComparisonService service = new DashboardMetricsShadowComparisonService(
        resolver, coordinator, modeProvider, telemetry, shutdownHandler, () -> captured);
    TenantTestHelper.initMultiTenantMode();

    Tenant firstTenant = TenantTestHelper.testAsNewTenant("dashboard-shadow-first", tenant -> {
      service.maybeSchedule(request(), indexResponse(1L, 1000L), Instant.EPOCH, 1000L);
    });
    Tenant secondTenant = TenantTestHelper.testAsNewTenant("dashboard-shadow-second", tenant -> {
      service.maybeSchedule(request(), indexResponse(1L, 1000L), Instant.EPOCH, 1000L);
    });
    ArgumentCaptor<TenantThreadPoolExecutor> executors =
        ArgumentCaptor.forClass(TenantThreadPoolExecutor.class);
    verify(shutdownHandler, times(2)).add(executors.capture());
    TenantThreadPoolExecutor firstExecutor = executors.getAllValues().get(0);
    TenantThreadPoolExecutor secondExecutor = executors.getAllValues().get(1);

    TenantTestHelper.testAsTenant(firstTenant, tenant -> {
      service.deregister();
      service.deregister();
    });

    assertThat(firstExecutor.isShutdown()).isTrue();
    assertThat(firstExecutor.getQueue()).isEmpty();
    assertThat(secondExecutor.isShutdown()).isFalse();
    verify(shutdownHandler).remove(firstExecutor);
    verify(shutdownHandler, never()).remove(secondExecutor);

    TenantTestHelper.testAsTenant(secondTenant, tenant -> service.deregister());
    assertThat(secondExecutor.isShutdown()).isTrue();
    assertThat(secondExecutor.getQueue()).isEmpty();
    verify(shutdownHandler).remove(secondExecutor);
  }

  @Test
  public void deregisterClearsCurrentTenantMismatchHistory() {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    when(resolver.resolve(any())).thenReturn(scope);
    when(coordinator.countApplications(scope)).thenReturn(new MetricValueDTO(1L, null, "sql"));
    Subject captured = mock(Subject.class);
    when(captured.associateWith(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ShutdownHandler shutdownHandler = mock(ShutdownHandler.class);
    DashboardMetricsShadowComparisonService service = new DashboardMetricsShadowComparisonService(
        resolver, coordinator, modeProvider, telemetry, shutdownHandler, () -> captured);
    TenantTestHelper.initMultiTenantMode();

    Tenant tenant = TenantTestHelper.testAsNewTenant("dashboard-shadow-cache", current -> {
      service.maybeSchedule(request(), indexResponse(2L, 1000L), Instant.ofEpochMilli(1500L), 1000L);
      verify(telemetry, timeout(5000)).recordMismatch(false);
      service.deregister();
    });
    TenantTestHelper.testAsTenant(tenant, current -> {
      service.maybeSchedule(request(), indexResponse(2L, 2000L), Instant.ofEpochMilli(2500L), 2000L);
      verify(telemetry, timeout(5000).times(2)).recordMismatch(false);
      verify(telemetry, never()).recordMismatch(true);
      service.deregister();
    });

    verify(shutdownHandler, times(2)).add(any(TenantThreadPoolExecutor.class));
    verify(shutdownHandler, times(2)).remove(any(TenantThreadPoolExecutor.class));
  }

  @Test
  public void logsContainHashesAndShapesButNoRawIds() {
    DashboardMetricsRequestDTO request = request();
    request.organizationIds = Set.of("raw-org-id");
    request.applicationIds = Set.of("raw-app-id");
    request.tagIds = Set.of("raw-tag-id");

    DashboardMetricsShadowCapture capture =
        DashboardMetricsShadowCapture.create(request, Instant.EPOCH, 1000L, indexResponse(1L, 1000L));

    assertThat(capture.filterHash()).matches("[0-9a-f]{64}");
    assertThat(capture.filterHash())
        .doesNotContain("raw-org-id")
        .doesNotContain("raw-app-id")
        .doesNotContain("raw-tag-id");
    assertThat(DashboardMetricsShadowComparisonService.principalShape(scope))
        .isEqualTo("RESTRICTED:OWNERS_1");
  }

  @Test
  public void scopeHashIsDeterministicCompleteAndPrivacySafe() {
    ResolvedScope reordered = new ResolvedScope(
        ResolvedScope.Kind.RESTRICTED,
        null,
        new java.util.LinkedHashSet<>(List.of("owner-b", "owner-a")),
        new java.util.LinkedHashSet<>(List.of("policy-owner-b", "policy-owner-a")),
        new java.util.LinkedHashSet<>(List.of("org-b", "org-a")),
        new java.util.LinkedHashSet<>(List.of("app-b", "app-a")),
        false);
    ResolvedScope sameScope = new ResolvedScope(
        ResolvedScope.Kind.RESTRICTED,
        null,
        Set.of("owner-a", "owner-b"),
        Set.of("policy-owner-a", "policy-owner-b"),
        Set.of("org-a", "org-b"),
        Set.of("app-a", "app-b"),
        false);
    ResolvedScope differentApplications = new ResolvedScope(
        ResolvedScope.Kind.RESTRICTED,
        null,
        Set.of("owner-a", "owner-b"),
        Set.of("policy-owner-a", "policy-owner-b"),
        Set.of("org-a", "org-b"),
        Set.of("app-other"),
        false);
    ResolvedScope differentPolicyOwners = new ResolvedScope(
        ResolvedScope.Kind.RESTRICTED,
        null,
        Set.of("owner-a", "owner-b"),
        Set.of("policy-owner-other"),
        Set.of("org-a", "org-b"),
        Set.of("app-a", "app-b"),
        false);

    String hash = DashboardMetricsShadowComparisonService.scopeHash(reordered);

    assertThat(hash).matches("[0-9a-f]{64}");
    assertThat(hash).isEqualTo(DashboardMetricsShadowComparisonService.scopeHash(sameScope));
    assertThat(hash).isNotEqualTo(DashboardMetricsShadowComparisonService.scopeHash(differentApplications));
    assertThat(hash).isNotEqualTo(DashboardMetricsShadowComparisonService.scopeHash(differentPolicyOwners));
    assertThat(hash).doesNotContain("owner-a", "org-a", "app-a");
  }

  @Test
  public void captureStoresStructurallyImmutableFiltersAndReconstructsWorkerRequest() {
    DashboardMetricsRequestDTO request = request();
    java.util.HashSet<String> organizations = new java.util.HashSet<>(Set.of("org-1"));
    request.organizationIds = organizations;
    DashboardMetricsShadowCapture capture =
        DashboardMetricsShadowCapture.create(request, Instant.EPOCH, 1000L, indexResponse(1L, 1000L));

    organizations.add("org-2");
    DashboardMetricsRequestDTO firstWorkerRequest = capture.toRequest();
    firstWorkerRequest.organizationIds = Set.of("mutated");
    DashboardMetricsRequestDTO secondWorkerRequest = capture.toRequest();

    assertThat(capture.organizationIds()).containsExactly("org-1");
    assertThat(secondWorkerRequest.organizationIds).containsExactly("org-1");
    assertThat(java.util.Arrays.stream(capture.getClass().getRecordComponents())
        .noneMatch(component -> component.getType().equals(DashboardMetricsRequestDTO.class)))
            .isTrue();
  }

  @Test
  public void firstMismatchIsRawOnly() {
    DashboardMetricsShadowComparisonService service = persistenceService(1L);

    service.maybeSchedule(request(), indexResponse(2L, 2000L), Instant.ofEpochMilli(1500L), 2000L);

    verify(telemetry).recordMismatch(false);
    verify(telemetry, never()).recordMismatch(true);
  }

  @Test
  public void secondMismatchAtSameSnapshotRemainsRawOnly() {
    DashboardMetricsShadowComparisonService service = persistenceService(1L);

    service.maybeSchedule(request(), indexResponse(2L, 1000L), Instant.ofEpochMilli(1500L), 1000L);
    service.maybeSchedule(request(), indexResponse(2L, 1000L), Instant.ofEpochMilli(1600L), 1000L);

    verify(telemetry, times(2)).recordMismatch(false);
    verify(telemetry, never()).recordMismatch(true);
  }

  @Test
  public void secondMismatchAfterSnapshotAdvancesIsPersistent() {
    DashboardMetricsShadowComparisonService service = persistenceService(1L);

    service.maybeSchedule(request(), indexResponse(2L, 1000L), Instant.ofEpochMilli(1500L), 1000L);
    service.maybeSchedule(request(), indexResponse(2L, 1600L), Instant.ofEpochMilli(1700L), 1600L);

    verify(telemetry).recordMismatch(false);
    verify(telemetry).recordMismatch(true);
  }

  @Test
  public void distinctRestrictedScopesDoNotShareMismatchObservations() {
    ResolvedScope firstScope = new ResolvedScope(
        ResolvedScope.Kind.RESTRICTED,
        null,
        Set.of("owner-a"),
        Set.of("policy-owner-a"),
        Set.of("org-a"),
        Set.of("app-a"),
        false);
    ResolvedScope secondScope = new ResolvedScope(
        ResolvedScope.Kind.RESTRICTED,
        null,
        Set.of("owner-b"),
        Set.of("policy-owner-b"),
        Set.of("org-b"),
        Set.of("app-b"),
        false);
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    when(resolver.resolve(any())).thenReturn(firstScope, secondScope);
    when(coordinator.countApplications(any())).thenReturn(new MetricValueDTO(1L, null, "sql"));
    DashboardMetricsShadowComparisonService service = service(ignored -> true, Runnable::run);

    service.maybeSchedule(request(), indexResponse(2L, 1000L), Instant.ofEpochMilli(1500L), 1000L);
    service.maybeSchedule(request(), indexResponse(2L, 1600L), Instant.ofEpochMilli(1700L), 1600L);

    verify(telemetry, times(2)).recordMismatch(false);
    verify(telemetry, never()).recordMismatch(true);
  }

  @Test
  public void missingSnapshotCanNeverBecomePersistent() {
    DashboardMetricsShadowComparisonService service = persistenceService(1L);

    service.maybeSchedule(request(), indexResponse(2L, null), Instant.ofEpochMilli(1500L), null);
    service.maybeSchedule(request(), indexResponse(2L, null), Instant.ofEpochMilli(1700L), null);

    verify(telemetry, times(2)).recordMismatch(false);
    verify(telemetry, never()).recordMismatch(true);
  }

  @Test
  public void laterMatchClearsRawObservation() {
    DashboardMetricsShadowComparisonService service = persistenceService(1L);

    service.maybeSchedule(request(), indexResponse(2L, 1000L), Instant.ofEpochMilli(1500L), 1000L);
    service.maybeSchedule(request(), indexResponse(1L, 1600L), Instant.ofEpochMilli(1700L), 1600L);
    service.maybeSchedule(request(), indexResponse(2L, 1800L), Instant.ofEpochMilli(1900L), 1800L);

    verify(telemetry, times(2)).recordMismatch(false);
    verify(telemetry, never()).recordMismatch(true);
  }

  @Test
  public void breakdownDifferenceIsAMismatch() {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    when(resolver.resolve(any())).thenReturn(scope);
    when(coordinator.countApplications(scope))
        .thenReturn(new MetricValueDTO(1L, Map.of("stages", 2L), "sql"));
    DashboardMetricsDTO served = new DashboardMetricsDTO(
        new MetricValueDTO(1L, Map.of("stages", 1L), "index"),
        null, null, null, null, null, null, null, 1000L);

    service(ignored -> true, Runnable::run)
        .maybeSchedule(request(), served, Instant.EPOCH, 1000L);

    verify(telemetry).recordMismatch(false);
  }

  @Test
  public void unavailableValuesAreSkippedWithoutCreatingMismatch() {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    when(resolver.resolve(any())).thenReturn(scope);
    when(coordinator.countApplications(scope)).thenReturn(MetricValueDTO.unavailable("sql"));

    service(ignored -> true, Runnable::run)
        .maybeSchedule(request(), indexResponse(1L, 1000L), Instant.EPOCH, 1000L);

    verify(telemetry, never()).recordMismatch(any(Boolean.class));
    verify(telemetry).recordShadowOutcome(ShadowOutcome.COMPLETED);
  }

  @Test
  public void metricFailureDoesNotAbortRemainingEligibleComparisons() {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    when(resolver.resolve(any())).thenReturn(scope);
    when(coordinator.countApplications(scope)).thenThrow(new IllegalStateException("application query failed"));
    when(coordinator.countOrganizations(scope)).thenReturn(new MetricValueDTO(1L, null, "sql"));
    DashboardMetricsDTO served = new DashboardMetricsDTO(
        new MetricValueDTO(1L, null, "index"),
        null,
        null,
        new MetricValueDTO(2L, null, "index"),
        null, null, null, null, 1000L);

    service(ignored -> true, Runnable::run)
        .maybeSchedule(request(), served, Instant.EPOCH, 1000L);

    verify(coordinator).countOrganizations(scope);
    verify(telemetry).recordMismatch(false);
    verify(telemetry).recordShadowOutcome(ShadowOutcome.SAMPLED);
    verify(telemetry).recordShadowOutcome(ShadowOutcome.FAILED);
    verify(telemetry, never()).recordShadowOutcome(ShadowOutcome.COMPLETED);
  }

  @Test
  public void denyAllScopeSkipsSqlComparisonsWithoutMismatch() {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    when(resolver.resolve(any())).thenReturn(ResolvedScope.denyAll(ResolvedScope.DenyReason.NO_ACCESS));

    service(ignored -> true, Runnable::run)
        .maybeSchedule(request(), indexResponse(1L, 1000L), Instant.EPOCH, 1000L);

    verify(coordinator, never()).countApplications(any());
    verify(coordinator, never()).countOrganizations(any());
    verify(coordinator, never()).countPolicies(any());
    verify(coordinator, never()).countViolations(any());
    verify(telemetry, never()).recordMismatch(any(Boolean.class));
    verify(telemetry).recordShadowOutcome(ShadowOutcome.COMPLETED);
  }

  @Test
  public void skippedCountIncludesUnsupportedMetricsPresentInCapturedTier() {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    DashboardMetricsDTO served = new DashboardMetricsDTO(
        MetricValueDTO.unsupported(List.of("stageIds")),
        null,
        null,
        MetricValueDTO.unsupported(List.of("stageIds")),
        null, null, null, null, 1000L);

    service(ignored -> true, Runnable::run)
        .maybeSchedule(request(), served, Instant.EPOCH, 1000L);

    assertThat(logOutput.getDebugMessages(DashboardMetricsShadowComparisonService.class.getName()))
        .anyMatch(message -> message.contains("tier=SUMMARY skipped=2"));
    verify(resolver, never()).resolve(any());
  }

  private DashboardMetricsShadowComparisonService persistenceService(final long sqlTotal) {
    when(modeProvider.shadowSampleDenominator()).thenReturn(OptionalInt.of(1));
    when(resolver.resolve(any())).thenReturn(scope);
    when(coordinator.countApplications(scope)).thenReturn(new MetricValueDTO(sqlTotal, null, "sql"));
    return service(ignored -> true, Runnable::run);
  }

  private void stubMatchingSql() {
    when(resolver.resolve(any())).thenReturn(scope);
    when(coordinator.countApplications(scope)).thenReturn(new MetricValueDTO(1L, null, "sql"));
  }

  private DashboardMetricsShadowComparisonService service(
      final java.util.function.IntPredicate sampler,
      final Executor executor)
  {
    Subject subject = mock(Subject.class);
    when(subject.associateWith(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    return service(sampler, executor, () -> subject);
  }

  private DashboardMetricsShadowComparisonService service(
      final java.util.function.IntPredicate sampler,
      final Executor executor,
      final java.util.function.Supplier<Subject> subjectSupplier)
  {
    return new DashboardMetricsShadowComparisonService(
        resolver, coordinator, modeProvider, telemetry, sampler, executor, subjectSupplier);
  }

  private static DashboardMetricsRequestDTO request() {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.includeHeavyMetrics = false;
    return request;
  }

  private static DashboardMetricsDTO indexResponse(final long applications, final Long snapshotTime) {
    return new DashboardMetricsDTO(
        new MetricValueDTO(applications, null, "index"),
        null, null, null, null, null, null, null, snapshotTime);
  }
}
