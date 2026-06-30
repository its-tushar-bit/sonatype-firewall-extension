/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.metrics.PolicyEvaluateServiceMetrics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtil;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationVirtualThreadExecutor;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.LongTaskTimer.Sample;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs policy re-evaluation ({@link ReportService#reUploadScanToHds} followed by
 * {@link ScanPolicyEvaluator#evaluate}) on a virtual thread, off the HTTP request thread. Progress is tracked
 * through the {@link PersistedPolicyEvaluationPollingResult} polling-result table, so callers poll for completion
 * via {@code PolicyEvaluateService#pollEvaluationResult} rather than holding the request open for the full HDS
 * report-regeneration window.
 */
@Named
@Singleton
public class AsyncReevaluationService
{
  private static final Logger log = LoggerFactory.getLogger(AsyncReevaluationService.class);

  private final ReportService reportService;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyEvaluationUtil policyEvaluationUtil;

  private final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  private final ErrorResponseGenerator errorResponseGenerator;

  private final PolicyEvaluateServiceMetrics policyEvaluateServiceMetrics;

  private final ExecutorService executor;

  @VisibleForTesting
  volatile boolean disablePollingIntervalForTesting = false;

  @Inject
  public AsyncReevaluationService(
      final ReportService reportService,
      final ScanPolicyEvaluator scanPolicyEvaluator,
      final PolicyEvaluationUtil policyEvaluationUtil,
      final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO,
      final ErrorResponseGenerator errorResponseGenerator,
      final PolicyEvaluateServiceMetrics policyEvaluateServiceMetrics,
      final ShutdownHandler shutdownHandler,
      @Nullable final MeterRegistry meterRegistry)
  {
    this.reportService = reportService;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyEvaluationUtil = policyEvaluationUtil;
    this.persistedPolicyEvaluationPollingResultDAO = persistedPolicyEvaluationPollingResultDAO;
    this.errorResponseGenerator = errorResponseGenerator;
    this.policyEvaluateServiceMetrics = policyEvaluateServiceMetrics;
    this.executor = new PolicyEvaluationVirtualThreadExecutor(meterRegistry, "policy_reevaluation",
        AsyncReevaluationService.class.getName());
    shutdownHandler.add(executor, ShutdownPriority.POLICY_EVALUATIONS);
  }

  /**
   * Starts an asynchronous re-evaluation for the given scan. Creates a {@code PENDING} polling-result row,
   * submits the HDS re-upload + policy evaluation to a virtual thread, and returns immediately. The caller
   * polls the returned {@link PolicyEvaluationReceipt#getStatusId() status id} for the outcome.
   *
   * @param application the application whose scan is being re-evaluated
   * @param scanId the scan to re-evaluate
   * @param skipAutoWaivers whether auto-waivers should be skipped during re-evaluation
   * @param clientUserAgent the originating client user agent, forwarded to HDS
   * @return a receipt carrying the status id used to poll for completion
   */
  public PolicyEvaluationReceipt startReevaluation(
      final Application application,
      final String scanId,
      final boolean skipAutoWaivers,
      final String clientUserAgent)
  {
    final String statusId = UUID.randomUUID().toString().replace("-", "");

    final PersistedPolicyEvaluationPollingResult persistedResult =
        policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultIfNeeded(application.getId(), statusId,
            disablePollingIntervalForTesting);

    log.debug("Submitting asynchronous re-evaluation task for app public id {}, scan id {}. The status ID is {}.",
        application.getPublicId(), scanId, statusId);

    AuditData.get()
        .continueAsync(
            new ReevaluateTask(application, scanId, skipAutoWaivers, clientUserAgent, persistedResult),
            executor::submit);

    final PolicyEvaluationReceipt receipt = new PolicyEvaluationReceipt();
    receipt.setStatusId(statusId);
    return receipt;
  }

  @VisibleForTesting
  ExecutorService getExecutor() {
    return executor;
  }

  private void updatePollingResult(
      final PersistedPolicyEvaluationPollingResult persistedResult,
      final PolicyEvaluationPollingResult result)
  {
    persistedResult.setPolicyEvaluationPollingResult(result);
    persistedPolicyEvaluationPollingResultDAO.update(persistedResult);
  }

  private final class ReevaluateTask
      implements Runnable
  {
    private final Application application;

    private final String scanId;

    private final boolean skipAutoWaivers;

    private final String clientUserAgent;

    private final PersistedPolicyEvaluationPollingResult persistedResult;

    private ReevaluateTask(
        final Application application,
        final String scanId,
        final boolean skipAutoWaivers,
        final String clientUserAgent,
        final PersistedPolicyEvaluationPollingResult persistedResult)
    {
      this.application = application;
      this.scanId = scanId;
      this.skipAutoWaivers = skipAutoWaivers;
      this.clientUserAgent = clientUserAgent;
      this.persistedResult = persistedResult;
    }

    @Override
    public void run() {
      // Default to a terminal FAILED state so that even if the catch block below throws before it can build its
      // own result (e.g. errorResponseGenerator.mapException throws), the finally still persists a terminal row
      // instead of leaving it PENDING until the client's deadline.
      PolicyEvaluationPollingResult result = new PolicyEvaluationPollingResult();
      result.setStatus(PolicyEvaluationStatus.FAILED);

      // Track async re-evaluations in metrics for parity with the synchronous CompleteEvaluationTask path, so their
      // in-flight count and duration are observable. The sample is null when no MeterRegistry is configured.
      final Sample sample = policyEvaluateServiceMetrics.emitStartPolicyEvaluation();
      try {
        final long start = System.currentTimeMillis();

        PolicyEvaluation policyEvaluation =
            reportService.reUploadScanToHds(application.getId(), scanId, clientUserAgent);
        Stage stage = new Stage(policyEvaluation.getStageTypeId());
        ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage,
            policyEvaluation.getScanTriggerType(), policyEvaluation.getClientScanType(), skipAutoWaivers);
        PolicyEvaluationResult policyEvaluationResult =
            scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation, results.allViolations, true);

        // Terminal result: no nextPollingIntervalInSeconds (the client stops polling on COMPLETED),
        // consistent with the synchronous evaluate path's completed result.
        result = new PolicyEvaluationPollingResult();
        result.setStatus(PolicyEvaluationStatus.COMPLETED);
        result.setResult(policyEvaluationResult);

        log.debug("Re-evaluated policy for app public id {}, scan id {} in {} ms. The status ID is {}.",
            application.getPublicId(), scanId, System.currentTimeMillis() - start, persistedResult.getStatusId());
      }
      catch (Exception e) {
        // Any failure - including interruption when the executor is shut down via shutdownNow() during a forced
        // server shutdown - is recorded as a terminal FAILED row (rather than left PENDING until the client's
        // deadline). To the user an interrupted re-evaluation and any other failed re-evaluation are the same
        // outcome, so they share this path.
        log.error("Failed to re-evaluate policy for app public id {}, scan id {}. The status ID is {}.",
            application.getPublicId(), scanId, persistedResult.getStatusId(), e);

        // Map once (non-logging variant: the contextual log.error above is the single failure log)
        // and reuse the message for both the polling result and the audit record.
        String reason = errorResponseGenerator.mapException(e).getMessageBody();
        result = new PolicyEvaluationPollingResult();
        result.setStatus(PolicyEvaluationStatus.FAILED);
        result.setReason(reason);
        AuditData.get().setException(new RuntimeException(reason, e));
      }
      finally {
        // Persist the terminal row first, then emit metrics: a throw from emitEndPolicyEvaluation must not skip the
        // write and leave the row PENDING (the guarantee this whole method is built around), so metrics come last.
        // Clear any interrupt flag for the duration of the DB write: on a forced shutdown the virtual thread may
        // already be interrupted, and a JDBC driver / connection pool can throw InterruptedException on a blocking
        // call if the flag is set, which would prevent the row from leaving PENDING.
        final boolean wasInterrupted = Thread.interrupted();
        try {
          updatePollingResult(persistedResult, result);
        }
        catch (Exception updateError) {
          // The re-evaluation work itself already committed; if persisting the terminal polling state fails the
          // row stays PENDING and the caller would poll until its deadline. Log so operators can detect this.
          log.error("Failed to persist re-evaluation polling result for app public id {}, scan id {}. "
              + "The status ID is {} and its row may remain PENDING despite the work finishing.",
              application.getPublicId(), scanId, persistedResult.getStatusId(), updateError);
        }

        // Emit metrics with the interrupt flag still cleared, so a metrics implementation that ever makes a blocking
        // call can't throw InterruptedException here. The flag is restored last, after all blocking work, so the
        // virtual thread still terminates promptly. Metrics are best-effort: a failure here must not propagate as an
        // uncaught exception on the virtual thread (which the executor would swallow unlogged), so log at WARN.
        try {
          policyEvaluateServiceMetrics.emitEndPolicyEvaluation(sample);
        }
        catch (Exception metricsError) {
          log.warn("Failed to emit re-evaluation completion metrics for app public id {}, scan id {}. "
              + "The status ID is {}; the terminal polling result was already persisted.",
              application.getPublicId(), scanId, persistedResult.getStatusId(), metricsError);
        }

        if (wasInterrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
