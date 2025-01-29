/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.componentanalysis;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.*;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.*;
import com.sonatype.insight.brain.policy.utils.EvaluationUtils;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Named
@Singleton
public class ComponentAnalysisService
    implements Managed
{
  private final ExecutorService executor;

  private static final Logger log = LoggerFactory.getLogger(ComponentAnalysisService.class);

  private final PolicyEvaluationUtil policyEvaluationUtil;

  private ApplicationDAO applicationDAO;

  private ScanHandler scanHandler;

  private ReportComponentService reportComponentService;

  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  public boolean disablePollingIntervalForTesting = false;

  private final ErrorResponseGenerator errorResponseGenerator;

  @Inject
  public ComponentAnalysisService(
      final PolicyEvaluationUtil policyEvaluationUtil,
      final ApplicationDAO applicationDAO,
      final ScanHandler scanHandler,
      final ReportComponentService reportComponentService,
      final ErrorResponseGenerator errorResponseGenerator
  )
  {
    this.executor = buildExecutorService();
    this.policyEvaluationUtil = policyEvaluationUtil;
    this.applicationDAO = applicationDAO;
    this.scanHandler = scanHandler;
    this.reportComponentService = reportComponentService;
    this.errorResponseGenerator = errorResponseGenerator;
  }

  private ExecutorService buildExecutorService() {
    /**
     * TODO: Add metrics to the executor service similar to what is done on the policy evaluation executor
     */

    return new PolicyEvaluationThreadPoolExecutor(ComponentAnalysisService.class.getName());
  }

  @VisibleForTesting
  ExecutorService getExecutor() {
    return executor;
  }

  @Override
  public void start() throws Exception {
    // no-op
  }

  @Override
  public void stop() throws Exception {
    executor.shutdown();
  }

  /**
   * Starts the component analysis for a given application, integration, stage and type. After
   * starting will
   * return a {@link PolicyEvaluationReceipt} for the requester to use to check on results
   * via
   *
   * @param integrationType {@link IntegrationType}
   * @param applicationPublicId public shared id
   * @param clientScanType {@link ClientScanType}
   * @param req {@link HttpServletRequest}
   * @param stage {@link Stage}
   * @return PolicyEvaluationReceipt
   * @throws IOException when the scan file, uploaded via the request, is unable to be read or processed
   *
   * @since 1.69
   */
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public PolicyEvaluationReceipt analyzeComponentsWithPolling(
      IntegrationType integrationType,
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ClientScanType clientScanType,
      HttpServletRequest req,
      Stage stage) throws IOException
  {
    EvaluationUtils.ensureNewEvaluationProcessEnabled();
    policyEvaluationUtil.validateEvaluationTypeAndFeature(integrationType, stage);

    String statusId = UUID.randomUUID().toString().replace("-", "");
    log.debug(
        "Received request to evaluate policy for app public id {}, clientScanType {}, stageTypeId {}. " +
            "The status ID of the operation is {}.",
        applicationPublicId, clientScanType, stage.getStageTypeId(), statusId);

    if (stage.getStageTypeId().equals(Stage.ID_COMPLIANCE)) {
      throw new BadRequestException(
          "Compliance scans are not supported for component analysis. Please use the policy evaluation endpoint.");
    }

    /**
     * TODO: Call triggerAsyncComponentAnalysis with the appropriate parameters
     */

    PolicyEvaluationReceipt policyEvaluationReceipt = new PolicyEvaluationReceipt();
    policyEvaluationReceipt.setStatusId(statusId);

    return policyEvaluationReceipt;
  }

  private void triggerAsyncComponentAnalysis() {
    /**
     * TODO: Execute ComponentAnalysisTask with the appropriate parameters
     */
    // no-op
  }

  /**
   * @since 1.69
   */
  class ComponentAnalysisTask
      extends EvaluationTask
  {
    private final Application app;

    private final ClientScanType clientScanType;

    private final Stage stage;

    private final String statusId;

    private final File tempScanFile;

    private final TelemetryData thirdPartyScanTelemetryData;

    private final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult;

    private final String clientUserAgent;

    private final ScanContext scanContext;

    private final long taskCreateTime = System.currentTimeMillis();

    ComponentAnalysisTask(
        final Application app,
        final ClientScanType clientScanType,
        final String statusId,
        final Stage stage,
        // final ScanTriggerType scanTriggerType,
        final File tempScanFile,
        final TelemetryData thirdPartyScanTelemetryData,
        final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult,
        final String clientUserAgent,
        final ScanContext scanContext)
    {
      // no-op
      this.app = app;
      this.clientScanType = clientScanType;
      this.statusId = statusId;
      this.tempScanFile = tempScanFile;
      this.thirdPartyScanTelemetryData = thirdPartyScanTelemetryData;
      this.persistedPolicyEvaluationPollingResult = persistedPolicyEvaluationPollingResult;
      this.clientUserAgent = clientUserAgent;
      this.scanContext = scanContext;
      this.stage = stage;
    }

    @Override
    public void run() {
      log.debug(
          "Component analysis task (appPublicId {}, statusId {}) waited in " +
              "queue for {} ms.",
          app.getPublicId(), statusId, System.currentTimeMillis() - taskCreateTime);

      String scanId = null;
      PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
      policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.PENDING);
      policyEvaluationPollingResult.setNextPollingIntervalInSeconds(
          getNextPollingInterval(disablePollingIntervalForTesting));

      try {
        ScanReceipt scanReceipt =
            scanHandler.handle(tempScanFile, app, clientScanType, thirdPartyScanTelemetryData, stage.getStageTypeId(),
                clientUserAgent, persistedPolicyEvaluationPollingResult.getStatusId(), scanContext);
        scanId = scanReceipt.getScanId();

        policyEvaluationPollingResult.setScanReceipt(scanReceipt);
        persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
        persistedPolicyEvaluationPollingResultDAO.update(persistedPolicyEvaluationPollingResult);

        final long start = System.currentTimeMillis();

        log.debug(
            "Evaluating policy for app public id {}, scan id {}, stageTypeId {}. The status ID of the operation is {}.",
            app.getPublicId(), scanId, stage.getStageTypeId(), statusId);

        // ReportComponentData reportComponentData =
        //    reportComponentService.fetchReportAndComponents(app, scanId);

        log.debug(
            "Evaluated policy for app public id {}, scan id {}, stageTypeId {} in {} ms."
                + " The status ID of the operation is {}.",
            app.getPublicId(), scanId, stage.getStageTypeId(), System.currentTimeMillis() - start, statusId);

        policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
        policyEvaluationPollingResult.setScanReceipt(scanReceipt);
        policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.COMPLETED);
        policyEvaluationPollingResult.setSubStatus(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);
      }
      catch (Exception e) {
        log.error(
            "Failed to evaluate policy for app public id {}, scan id {}, stageTypeId {}." +
                " The status ID of the operation is {}.",
            app.getPublicId(), scanId, stage.getStageTypeId(), statusId, e);
        // in failed status, hold onto as much as we have obtained so far
        policyEvaluationPollingResult = makeCopy(policyEvaluationPollingResult);
        policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.FAILED);
        policyEvaluationPollingResult.setSubStatus(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING);
        policyEvaluationPollingResult.setReason(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody());
        AuditData.get()
            .setException(new RuntimeException(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody(), e));
      }

      persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
      persistedPolicyEvaluationPollingResultDAO.update(persistedPolicyEvaluationPollingResult);
    }
  }
}
