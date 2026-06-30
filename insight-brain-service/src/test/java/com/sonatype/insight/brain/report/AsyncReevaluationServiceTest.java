/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.metrics.PolicyEvaluateServiceMetrics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtil;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.jaxrs.error.ErrorResponse;

import org.junit.Test;
import org.mockito.Mock;

public class AsyncReevaluationServiceTest
    extends AbstractComponentTest
{
  private static final String APP_ID = "app-1";

  private static final String SCAN_ID = "scan-1";

  private static final String USER_AGENT = "test-agent";

  private final Application application = newApplication();

  @Mock
  private ReportService reportService;

  @Mock
  private ScanPolicyEvaluator scanPolicyEvaluator;

  @Mock
  private PolicyEvaluationUtil policyEvaluationUtil;

  @Mock
  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @Mock
  private ErrorResponseGenerator errorResponseGenerator;

  @Test
  public void testAsyncReevaluationService_AddsExecutorToShutdownHandler() {
    ShutdownHandler mockShutdownHandler = mock(ShutdownHandler.class);
    AsyncReevaluationService localService = new AsyncReevaluationService(
        lookup(ReportService.class),
        lookup(ScanPolicyEvaluator.class),
        lookup(PolicyEvaluationUtil.class),
        lookup(PersistedPolicyEvaluationPollingResultDAO.class),
        mock(ErrorResponseGenerator.class),
        new PolicyEvaluateServiceMetrics(null /* meterRegistry */),
        mockShutdownHandler,
        null /* meterRegistry */);

    try {
      verify(mockShutdownHandler).add(localService.getExecutor(), ShutdownPriority.POLICY_EVALUATIONS);
    }
    finally {
      localService.getExecutor().shutdownNow();
    }
  }

  @Test
  public void testStartReevaluation_Success_PersistsCompletedResult() throws Exception {
    PersistedPolicyEvaluationPollingResult persisted = stubPendingPollingResult();

    PolicyEvaluation reUploaded = new PolicyEvaluation();
    reUploaded.setStageTypeId("build");
    when(reportService.reUploadScanToHds(APP_ID, SCAN_ID, USER_AGENT)).thenReturn(reUploaded);

    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = new PolicyEvaluation();
    results.allViolations = List.of();
    when(scanPolicyEvaluator.evaluate(same(application), eq(SCAN_ID), any(Stage.class), any(), any(),
        eq(false))).thenReturn(results);

    // A distinguishing field that lets us confirm this exact result round-tripped into the persisted row.
    // (The row stores the result as JSON, so identity comparison is not meaningful - assert by value.)
    PolicyEvaluationResult evaluationResult = new PolicyEvaluationResult();
    evaluationResult.setSeverePolicyViolationCount(7);
    when(scanPolicyEvaluator.createPolicyEvaluationResult(any(PolicyEvaluation.class), anyList(),
        eq(true))).thenReturn(evaluationResult);

    runReevaluationToCompletion();

    PolicyEvaluationPollingResult finalResult = persisted.getPolicyEvaluationPollingResult();
    assertThat(finalResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(finalResult.getResult()).isNotNull();
    assertThat(finalResult.getResult().getSeverePolicyViolationCount()).isEqualTo(7);
  }

  /**
   * Graceful shutdown contract: the executor is registered with the {@link ShutdownHandler}, which on an orderly
   * shutdown calls {@code shutdown()} (not {@code shutdownNow()}) and awaits termination - so an in-flight
   * re-evaluation must be allowed to finish and persist {@code COMPLETED} rather than being interrupted into
   * {@code FAILED}. (Forced interruption - {@code shutdownNow()} after the grace period elapses - is covered by the
   * {@code _Interrupted_} tests.) Here the work blocks until after graceful {@code shutdown()} has been requested,
   * then proceeds; the task completing normally proves graceful shutdown waited.
   */
  @Test
  public void testStartReevaluation_GracefulShutdown_WaitsForInFlightTaskToComplete() throws Exception {
    PersistedPolicyEvaluationPollingResult persisted = stubPendingPollingResult();

    CountDownLatch evaluationStarted = new CountDownLatch(1);
    CountDownLatch shutdownRequested = new CountDownLatch(1);

    PolicyEvaluation reUploaded = new PolicyEvaluation();
    reUploaded.setStageTypeId("build");
    when(reportService.reUploadScanToHds(APP_ID, SCAN_ID, USER_AGENT)).thenAnswer(invocation -> {
      // Signal that the task is in flight, then block until graceful shutdown has been requested. If graceful
      // shutdown interrupted us (i.e. used shutdownNow), this await would throw InterruptedException and the task
      // would end FAILED instead of COMPLETED.
      evaluationStarted.countDown();
      assertThat(shutdownRequested.await(10, TimeUnit.SECONDS)).isTrue();
      return reUploaded;
    });

    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = new PolicyEvaluation();
    results.allViolations = List.of();
    when(scanPolicyEvaluator.evaluate(same(application), eq(SCAN_ID), any(Stage.class), any(), any(),
        eq(false))).thenReturn(results);
    when(scanPolicyEvaluator.createPolicyEvaluationResult(any(PolicyEvaluation.class), anyList(),
        eq(true))).thenReturn(new PolicyEvaluationResult());

    AsyncReevaluationService service = newService();
    service.startReevaluation(application, SCAN_ID, false, USER_AGENT);

    ExecutorService executor = service.getExecutor();
    try {
      assertThat(evaluationStarted.await(10, TimeUnit.SECONDS)).as("task started").isTrue();

      // Graceful shutdown while the task is mid-flight, mirroring ExecutorServiceShutdownRequest.
      executor.shutdown();
      shutdownRequested.countDown();

      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).as("in-flight task finished").isTrue();
    }
    finally {
      executor.shutdownNow();
    }

    // Graceful shutdown let the task run to completion rather than interrupting it.
    assertThat(persisted.getPolicyEvaluationPollingResult().getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    verifyNoInteractions(errorResponseGenerator);
  }

  @Test
  public void testStartReevaluation_EvaluationFails_PersistsFailedResultWithMappedReason() throws Exception {
    PersistedPolicyEvaluationPollingResult persisted = stubPendingPollingResult();

    when(reportService.reUploadScanToHds(APP_ID, SCAN_ID, USER_AGENT)).thenThrow(new RuntimeException("boom"));
    when(errorResponseGenerator.mapException(any())).thenReturn(new ErrorResponse(500, "mapped reason"));

    runReevaluationToCompletion();

    assertThat(persisted.getPolicyEvaluationPollingResult().getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(persisted.getPolicyEvaluationPollingResult().getReason()).isEqualTo("mapped reason");
  }

  /**
   * Guards the FAILED-default initialization: if the catch block itself throws before it can build its own result
   * (here {@code mapException} explodes), the {@code finally} must still persist a terminal FAILED row rather than
   * leaving the row stuck in PENDING until the client's polling deadline.
   */
  @Test
  public void testStartReevaluation_CatchBlockThrows_StillPersistsFailedResult() throws Exception {
    PersistedPolicyEvaluationPollingResult persisted = stubPendingPollingResult();

    when(reportService.reUploadScanToHds(APP_ID, SCAN_ID, USER_AGENT)).thenThrow(new RuntimeException("boom"));
    when(errorResponseGenerator.mapException(any())).thenThrow(new RuntimeException("mapper exploded"));

    runReevaluationToCompletion();

    assertThat(persisted.getPolicyEvaluationPollingResult().getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(persisted.getPolicyEvaluationPollingResult().getReason()).isNull();
  }

  /**
   * Interruption (e.g. the executor is shut down via {@code shutdownNow()} during a forced server shutdown) is
   * treated as any other failure: the task persists a terminal FAILED row with the mapped reason rather than
   * leaving it PENDING until the client's polling deadline.
   */
  @Test
  public void testStartReevaluation_Interrupted_PersistsFailedResultWithMappedReason() throws Exception {
    PersistedPolicyEvaluationPollingResult persisted = stubPendingPollingResult();

    // Interruption surfaces wrapped: a blocking call throws InterruptedException, which the evaluation stack
    // re-throws as an unchecked exception carrying it as the cause.
    when(reportService.reUploadScanToHds(APP_ID, SCAN_ID, USER_AGENT))
        .thenThrow(new RuntimeException("interrupted", new InterruptedException("interrupted")));
    when(errorResponseGenerator.mapException(any())).thenReturn(new ErrorResponse(500, "mapped reason"));

    runReevaluationToCompletion();

    assertThat(persisted.getPolicyEvaluationPollingResult().getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(persisted.getPolicyEvaluationPollingResult().getReason()).isEqualTo("mapped reason");
  }

  /**
   * Guards that the terminal-state persistence still runs when the task was interrupted: the {@code finally} clears
   * the interrupt flag around the blocking DB write so a JDBC driver can't throw {@code InterruptedException} on it and
   * leave the row stuck PENDING. We assert the DAO update actually ran (terminal row persisted) and observed a cleared
   * interrupt flag during the write.
   */
  @Test
  public void testStartReevaluation_Interrupted_ClearsInterruptFlagForTerminalWrite() throws Exception {
    PersistedPolicyEvaluationPollingResult persisted = stubPendingPollingResult();

    // Simulate a real forced-shutdown interrupt: set the running thread's interrupt flag, as shutdownNow() would,
    // so the finally has a flag to clear before the blocking DB write.
    when(reportService.reUploadScanToHds(APP_ID, SCAN_ID, USER_AGENT)).thenAnswer(invocation -> {
      Thread.currentThread().interrupt();
      throw new RuntimeException("interrupted", new InterruptedException("interrupted"));
    });
    when(errorResponseGenerator.mapException(any())).thenReturn(new ErrorResponse(500, "mapped reason"));

    AtomicBoolean interruptedDuringWrite = new AtomicBoolean(true);
    doAnswer(invocation -> {
      // The interrupt flag must be cleared for the duration of the blocking write so a JDBC driver can't abort it.
      interruptedDuringWrite.set(Thread.currentThread().isInterrupted());
      return null;
    }).when(persistedPolicyEvaluationPollingResultDAO).update(same(persisted));

    runReevaluationToCompletion();

    assertThat(persisted.getPolicyEvaluationPollingResult().getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(interruptedDuringWrite.get()).as("interrupt flag cleared during terminal DB write").isFalse();
    verify(persistedPolicyEvaluationPollingResultDAO).update(same(persisted));
  }

  /**
   * Guards the persist-before-metrics ordering and that metrics are best-effort: the terminal-state DB write must run
   * even if emitting end metrics throws (so a metrics failure cannot leave the row stuck PENDING - the guarantee this
   * whole method is built around), and the throw must be caught rather than escaping as an uncaught exception on the
   * virtual thread (which the executor would swallow unlogged).
   */
  @Test
  public void testStartReevaluation_MetricsEmitThrows_StillPersistsTerminalResult() throws Exception {
    PersistedPolicyEvaluationPollingResult persisted = stubPendingPollingResult();

    PolicyEvaluation reUploaded = new PolicyEvaluation();
    reUploaded.setStageTypeId("build");
    when(reportService.reUploadScanToHds(APP_ID, SCAN_ID, USER_AGENT)).thenReturn(reUploaded);
    ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
    results.evaluation = new PolicyEvaluation();
    results.allViolations = List.of();
    when(scanPolicyEvaluator.evaluate(same(application), eq(SCAN_ID), any(Stage.class), any(), any(),
        eq(false))).thenReturn(results);
    when(scanPolicyEvaluator.createPolicyEvaluationResult(any(PolicyEvaluation.class), anyList(),
        eq(true))).thenReturn(new PolicyEvaluationResult());

    PolicyEvaluateServiceMetrics throwingMetrics = mock(PolicyEvaluateServiceMetrics.class);
    doThrow(new RuntimeException("metrics boom")).when(throwingMetrics).emitEndPolicyEvaluation(any());

    runReevaluationToCompletion(newService(throwingMetrics));

    // The terminal row was persisted (COMPLETED) despite the metrics emit throwing afterwards, and the throw was
    // caught (the emit was reached) rather than escaping the task.
    assertThat(persisted.getPolicyEvaluationPollingResult().getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    verify(persistedPolicyEvaluationPollingResultDAO).update(same(persisted));
    verify(throwingMetrics).emitEndPolicyEvaluation(any());
  }

  private static Application newApplication() {
    Application application = new Application();
    application.setId(APP_ID);
    application.setPublicId("app-public-1");
    return application;
  }

  private PersistedPolicyEvaluationPollingResult stubPendingPollingResult() {
    PolicyEvaluationPollingResult pending = new PolicyEvaluationPollingResult();
    pending.setStatus(PolicyEvaluationStatus.PENDING);
    PersistedPolicyEvaluationPollingResult persisted =
        new PersistedPolicyEvaluationPollingResult(APP_ID, "status-1", pending);
    when(policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultIfNeeded(eq(APP_ID), anyString(),
        anyBoolean())).thenReturn(persisted);
    return persisted;
  }

  private AsyncReevaluationService newService() {
    return newService(new PolicyEvaluateServiceMetrics(null /* meterRegistry */));
  }

  private AsyncReevaluationService newService(final PolicyEvaluateServiceMetrics metrics) {
    return new AsyncReevaluationService(
        reportService,
        scanPolicyEvaluator,
        policyEvaluationUtil,
        persistedPolicyEvaluationPollingResultDAO,
        errorResponseGenerator,
        metrics,
        mock(ShutdownHandler.class),
        null /* meterRegistry */);
  }

  private void runReevaluationToCompletion() throws InterruptedException {
    runReevaluationToCompletion(newService());
  }

  private void runReevaluationToCompletion(final AsyncReevaluationService service) throws InterruptedException {
    service.startReevaluation(application, SCAN_ID, false, USER_AGENT);

    ExecutorService executor = service.getExecutor();
    executor.shutdown();
    try {
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
    finally {
      executor.shutdownNow();
    }
  }
}
