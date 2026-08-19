/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.componentanalysis;

import static java.util.Collections.singletonMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.*;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.*;
import com.sonatype.insight.brain.policy.utils.EvaluationUtils;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;

@Named
@Singleton
public class ComponentAnalysisService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(ComponentAnalysisService.class);

  private final ExecutorService executor;

  private final PolicyEvaluationUtil policyEvaluationUtil;

  private ApplicationDAO applicationDAO;

  private ScanHandler scanHandler;

  private ReportComponentService reportComponentService;

  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  private final ErrorResponseGenerator errorResponseGenerator;

  private final TelemetryUtils telemetryUtils;

  private boolean disablePollingIntervalForTesting = false;

  private final TelemetrySender telemetrySender;

  private final MeterRegistry meterRegistry;

  @Inject
  public ComponentAnalysisService(
      final PolicyEvaluationUtil policyEvaluationUtil,
      final ApplicationDAO applicationDAO,
      final ScanHandler scanHandler,
      final ReportComponentService reportComponentService,
      final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO,
      final ErrorResponseGenerator errorResponseGenerator,
      final TelemetryUtils telemetryUtils,
      final TelemetrySender telemetrySender,
      @Nullable final MeterRegistry meterRegistry)
  {
    this.meterRegistry = meterRegistry;
    this.executor = buildExecutorService();
    this.policyEvaluationUtil = policyEvaluationUtil;
    this.applicationDAO = applicationDAO;
    this.scanHandler = scanHandler;
    this.reportComponentService = reportComponentService;
    this.persistedPolicyEvaluationPollingResultDAO = persistedPolicyEvaluationPollingResultDAO;
    this.errorResponseGenerator = errorResponseGenerator;
    this.telemetryUtils = telemetryUtils;
    this.telemetrySender = telemetrySender;
  }

  private ExecutorService buildExecutorService() {
    return new PolicyEvaluationVirtualThreadExecutor(meterRegistry, "component_analysis",
        ComponentAnalysisService.class.getName());
  }

  @VisibleForTesting
  ExecutorService getExecutor() {
    return executor;
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
   * @param request {@link HttpServletRequest}
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
      HttpServletRequest request,
      Stage stage) throws IOException
  {
    policyEvaluationUtil.validateEvaluationTypeAndFeature(integrationType, stage);

    String statusId = UUID.randomUUID().toString().replace("-", "");
    log.debug(
        "Received request to analyze components for app public id {}, clientScanType {}, stageTypeId {}. " +
            "The status ID of the operation is {}.",
        applicationPublicId, clientScanType, stage.getStageTypeId(), statusId);

    if (stage.getStageTypeId().equals(Stage.ID_COMPLIANCE)) {
      throw new BadRequestException(
          "Compliance scans are not supported for component analysis. Please use the policy evaluation endpoint.");
    }

    triggerAsyncComponentAnalysis(integrationType, request, applicationPublicId, clientScanType, statusId, stage, null);

    PolicyEvaluationReceipt policyEvaluationReceipt = new PolicyEvaluationReceipt();
    policyEvaluationReceipt.setStatusId(statusId);

    return policyEvaluationReceipt;
  }

  private void triggerAsyncComponentAnalysis(
      final IntegrationType integrationType,
      final HttpServletRequest request,
      final String appPublicId,
      final ClientScanType clientScanType,
      final String statusId,
      final Stage stage,
      final ScanContext scanContext) throws IOException
  {
    final Application app = applicationDAO.getByPublicIdNotNull(appPublicId);
    final String thirdPartyScanType =
        clientScanType == ClientScanType.SONATYPE_THIRD_PARTY ? integrationType.toString() : null;
    final ScanTriggerType scanTriggerType = EvaluationUtils.getScanTriggerType(integrationType);
    final String clientUserAgent = HdsClient.getClientUserAgent(request);
    final TelemetryData thirdPartyScanTelemetryData = telemetryUtils.buildThirdPartyScanTelemetryData(
        app.getId(), stage, thirdPartyScanType, scanTriggerType, clientUserAgent);
    final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultWithSubStatusIfNeeded(app.getId(),
            statusId, disablePollingIntervalForTesting);
    final ScanEntity tempScanEntity = scanHandler.createTempScanFile(request, app);

    log.debug(
        "Submitting component analysis task for app public id {}, clientScanType {}, stageTypeId {}. "
            + "The status ID of the operation is {}.",
        app.getPublicId(), clientScanType, stage.getStageTypeId(), statusId);

    AuditData.get()
        .continueAsync(
            new ComponentAnalysisTask(
                app,
                clientScanType,
                scanTriggerType,
                statusId,
                stage,
                tempScanEntity,
                thirdPartyScanTelemetryData,
                persistedPolicyEvaluationPollingResult,
                clientUserAgent,
                scanContext),
            executor::submit);
  }

  private void sendEvaluationTelemetry(
      final String scanId,
      final String applicationId,
      final String stageId,
      final ScanTriggerType scanTriggerType,
      final Collection<Component> components,
      final String clientUserAgent)
  {
    TelemetryData telemetryData = telemetryUtils.buildComponentsAnalysisTelemetryData(
        scanId,
        applicationId,
        stageId,
        scanTriggerType,
        clientUserAgent,
        null,
        singletonMap("component_counts", getTelemetryComponentCounts(components)));
    telemetrySender.send(telemetryData);
  }

  private Map<String, Long> getTelemetryComponentCounts(final Collection<Component> components) {
    return components.stream()
        .map(Component::getComponentIdentifier)
        .map(componentIdentifier -> componentIdentifier == null ? "unknown" : componentIdentifier.getFormat())
        .collect(groupingBy(identity(), counting()));
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

    private final ScanTriggerType scanTriggerType;

    private final String statusId;

    private final ScanEntity tempScanEntity;

    private final TelemetryData thirdPartyScanTelemetryData;

    private final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult;

    private final String clientUserAgent;

    private final ScanContext scanContext;

    private final long taskCreateTime = System.currentTimeMillis();

    ComponentAnalysisTask(
        final Application app,
        final ClientScanType clientScanType,
        final ScanTriggerType scanTriggerType,
        final String statusId,
        final Stage stage,
        final ScanEntity tempScanEntity,
        final TelemetryData thirdPartyScanTelemetryData,
        final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult,
        final String clientUserAgent,
        final ScanContext scanContext)
    {
      this.app = app;
      this.clientScanType = clientScanType;
      this.scanTriggerType = scanTriggerType;
      this.statusId = statusId;
      this.tempScanEntity = tempScanEntity;
      this.thirdPartyScanTelemetryData = thirdPartyScanTelemetryData;
      this.persistedPolicyEvaluationPollingResult = persistedPolicyEvaluationPollingResult;
      this.clientUserAgent = clientUserAgent;
      this.scanContext = scanContext;
      this.stage = stage;
    }

    @Override
    public void run() {
      log.debug(
          "Component analysis task (appPublicId {}, statusId {}) waited in queue for {} ms.",
          app.getPublicId(), statusId, System.currentTimeMillis() - taskCreateTime);

      String scanId = null;
      PolicyEvaluationPollingResult policyEvaluationPollingResult =
          persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();

      try {
        ScanReceipt scanReceipt = scanHandler.handle(ScanHandler.ScanRequest.builder()
            .scanEntity(tempScanEntity)
            .application(app)
            .clientScanType(clientScanType)
            .thirdPartyScanTelemetryData(thirdPartyScanTelemetryData)
            .stageTypeId(stage.getStageTypeId())
            .clientUserAgent(clientUserAgent)
            .scanRequestId(persistedPolicyEvaluationPollingResult.getStatusId())
            .scanContext(scanContext)
            .build());
        scanId = scanReceipt.getScanId();

        policyEvaluationPollingResult.setScanReceipt(scanReceipt);
        updatePolicyEvaluationPollingResult(persistedPolicyEvaluationPollingResult, policyEvaluationPollingResult);

        final long start = System.currentTimeMillis();

        log.debug(
            "Loading report component data for app public id {}, scan id {}, stageTypeId {}. The status ID of the " +
                "operation is {}.",
            app.getPublicId(), scanId, stage.getStageTypeId(), statusId);

        // HDS will asynchronously write the report data to the report JSON files
        ReportComponentData reportComponentData =
            reportComponentService.fetchReportAndComponents(app, scanId, stage.getStageTypeId());

        log.debug(
            "Loaded report component data for app public id {}, scan id {}, stageTypeId {} in {} ms."
                + " The status ID of the operation is {}.",
            app.getPublicId(), scanId, stage.getStageTypeId(), System.currentTimeMillis() - start, statusId);

        policyEvaluationPollingResult = persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();
        policyEvaluationPollingResult.setSubStatus(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE);

        sendEvaluationTelemetry(
            scanId,
            app.getId(),
            stage.getStageTypeId(),
            scanTriggerType,
            reportComponentData.components,
            clientUserAgent);
      }
      catch (Exception e) {
        log.error(
            "Failed to load report component data for app public id {}, scan id {}, stageTypeId {}." +
                " The status ID of the operation is {}.",
            app.getPublicId(), scanId, stage.getStageTypeId(), statusId, e);
        // in failed status, hold onto as much as we have obtained so far
        policyEvaluationPollingResult = makeCopy(policyEvaluationPollingResult);
        policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.FAILED);
        policyEvaluationPollingResult.setReason(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody());
        AuditData.get()
            .setException(new RuntimeException(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody(), e));
      }

      updatePolicyEvaluationPollingResult(persistedPolicyEvaluationPollingResult, policyEvaluationPollingResult);
    }

    private void updatePolicyEvaluationPollingResult(
        final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult,
        final PolicyEvaluationPollingResult policyEvaluationPollingResult)
    {
      persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
      persistedPolicyEvaluationPollingResultDAO.update(persistedPolicyEvaluationPollingResult);
    }
  }
}
