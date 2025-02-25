/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.metrics.PolicyEvaluateServiceMetrics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.policy.componentanalysis.ComponentAnalysisService;
import com.sonatype.insight.brain.policy.utils.EvaluationUtils;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.lifecycle.Managed;
import io.micrometer.core.instrument.LongTaskTimer.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE;

@Named
@Singleton
/**
 * Service for policy evaluations for applications.
 */
public class PolicyEvaluateService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluateService.class);

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private final ApplicationDAO applicationDAO;

  private final ErrorResponseGenerator errorResponseGenerator;

  private final ExecutorService executor;

  private final ScanHandler scanHandler;

  private final StageTypeService stageTypeService;

  private final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  private final InsightWork insightWork;

  private final TelemetryUtils telemetryUtils;

  private final PolicyEvaluateServiceMetrics policyEvaluateServiceMetrics;

  private final PolicyEvaluationUtil policyEvaluationUtil;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ProductLicense productLicense;

  private final FeaturesService featuresService;

  public boolean disablePollingIntervalForTesting = false;

  @Inject
  public PolicyEvaluateService(
      ScanPolicyEvaluator scanPolicyEvaluator,
      PolicyAlertNotifier policyAlertNotifier,
      ErrorResponseGenerator errorResponseGenerator,
      ScanHandler scanHandler,
      StageTypeService stageTypeService,
      PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO,
      ApplicationDAO applicationDAO,
      InsightWork insightWork,
      TelemetryUtils telemetryUtils,
      ShutdownHandler shutdownHandler,
      PolicyEvaluateServiceMetrics policyEvaluateServiceMetrics,
      PolicyEvaluationUtil policyEvaluationUtil,
      SbomMetadataUtils sbomMetadataUtils,
      ProductLicense productLicense,
      FeaturesService featuresService)
  {
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.errorResponseGenerator = errorResponseGenerator;
    this.scanHandler = scanHandler;
    this.persistedPolicyEvaluationPollingResultDAO = persistedPolicyEvaluationPollingResultDAO;
    this.applicationDAO = applicationDAO;
    this.insightWork = insightWork;
    this.telemetryUtils = telemetryUtils;
    this.policyEvaluateServiceMetrics = policyEvaluateServiceMetrics;
    this.policyEvaluationUtil = policyEvaluationUtil;
    this.productLicense = productLicense;
    this.featuresService = featuresService;
    this.executor = buildExecutorService();
    this.stageTypeService = stageTypeService;
    this.sbomMetadataUtils = sbomMetadataUtils;
    shutdownHandler.add(executor, ShutdownPriority.POLICY_EVALUATIONS);
  }

  private ExecutorService buildExecutorService() {
    policyEvaluateServiceMetrics
        .registerGaugePolicyEvaluationThreadUtilization(PolicyEvaluationThreadPoolExecutor.THREAD_POOL_SIZE);

    return policyEvaluateServiceMetrics
        .registerAndGetTimedPolicyEvaluationExecutor(
            new PolicyEvaluationThreadPoolExecutor(PolicyEvaluateService.class.getName()));
  }

  // Visible for testing
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

  // default access for testing
  PolicyEvaluationResult evaluate(
      Application application,
      String scanId,
      Stage stage,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String clientInstanceId,
      ClientScanType clientScanType)
      throws IOException
  {
    ScanPolicyEvaluatorResults results =
        evaluateAndSendNotifications(application, scanId, stage, scanTriggerType, clientUserAgent, clientInstanceId,
            clientScanType);

    return scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation,
        results.allViolations, true);
  }

  PolicyEvaluationResult evaluate(
      Application application,
      String scanId,
      Stage stage,
      ScanTriggerType scanTriggerType)
      throws IOException
  {
    return evaluate(application, scanId, stage, scanTriggerType, null, null, null);
  }

  private ScanPolicyEvaluatorResults evaluateAndSendNotifications(
      Application application,
      String scanId,
      Stage stage,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String clientInstanceId,
      ClientScanType clientScanType) throws IOException
  {
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, scanTriggerType, clientUserAgent, clientInstanceId,
            clientScanType);

    if (!results.evaluation.isReevaluation()) {
      policyAlertNotifier.sendNotifications(application, results);
    }

    return results;
  }

  /**
   * Evaluate an Application by it's public Application id, Scan id and {@link Stage}
   *
   * @param applicationPublicId public shared id
   * @param scanId the id of the scan
   * @param stage {@link Stage}
   * @param scanTriggerType The trigger type for the scan for this evaluation {@link ScanTriggerType}
   */
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public PolicyEvaluationResult evaluate(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String scanId,
      Stage stage,
      ScanTriggerType scanTriggerType) throws IOException
  {
    log.debug("Received request to evaluate policy for app public id {}, scan id {}, stageTypeId {}",
        applicationPublicId, scanId, stage.getStageTypeId());

    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    if (!scanFile.exists()) {
      throw new NotFoundException("Cannot find scan with ID " + scanId);
    }

    return evaluate(app, scanId, stage, scanTriggerType);
  }

  /**
   * Starts the evaluation of an application, integration, type and stage. After starting will
   * return a {@link PolicyEvaluationReceipt} for the requester to use to check on results
   * via {@link #pollEvaluationResult(String, String)}
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
  public PolicyEvaluationReceipt evaluateWithPolling(
      IntegrationType integrationType,
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ClientScanType clientScanType,
      HttpServletRequest req,
      Stage stage) throws IOException
  {
    policyEvaluationUtil.validateEvaluationTypeAndFeature(integrationType, stage);

    String statusId = UUID.randomUUID().toString().replace("-", "");
    log.debug(
        "Received request to evaluate policy for app public id {}, clientScanType {}, stageTypeId {}. " +
            "The status ID of the operation is {}.",
        applicationPublicId, clientScanType, stage.getStageTypeId(), statusId);

    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    File tempScanFile = scanHandler.createTempScanFile(req, app);

    String thirdPartyScanType =
        clientScanType == ClientScanType.SONATYPE_THIRD_PARTY ? integrationType.toString() : null;

    validateLicenseLimits(stage);

    evaluateWithPolling(statusId, app, clientScanType, stage,
        EvaluationUtils.getScanTriggerType(integrationType), tempScanFile, thirdPartyScanType,
        HdsClient.getClientUserAgent(req), HdsClient.getClientInstanceId(req), null);

    PolicyEvaluationReceipt policyEvaluationReceipt = new PolicyEvaluationReceipt();
    policyEvaluationReceipt.setStatusId(statusId);

    return policyEvaluationReceipt;
  }

  /**
   * Starts the evaluation of an {@link Application}, {@link ScanTriggerType}, and {@link Stage} with polling. After
   * starting, it will return a {@link PolicyEvaluationReceipt} for the requester to use to check on results via
   * {@link #pollEvaluationResult(String, String)}.
   *
   * @param integrationType           - the type of integration {@link IntegrationType}
   * @param applicationPublicId       - the public shared ID of the application
   * @param clientScanType            - the type of client scan {@link ClientScanType}
   * @param req                       - the HTTP servlet request {@link HttpServletRequest}
   * @param stage                     - the stage of the evaluation {@link Stage}
   * @param statusId                  - the status ID of a previous evaluation
   * @param analysisDTO               - the vulnerability signature analysis data transfer object
   *                                  {@link VulnerabilitySignatureAnalysisDTO}
   * @return a receipt for the policy evaluation {@link PolicyEvaluationReceipt}
   */
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public PolicyEvaluationReceipt evaluateWithPolling(
      final IntegrationType integrationType,
      final @AuthzContext(Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      final ClientScanType clientScanType,
      final HttpServletRequest req,
      final Stage stage,
      final String statusId,
      final VulnerabilitySignatureAnalysisDTO analysisDTO)
  {
    return evaluateWithPolling(
        integrationType,
        applicationPublicId,
        clientScanType,
        req,
        stage,
        EvaluationUtils.getScanTriggerType(integrationType),
        HdsClient.getClientUserAgent(req),
        HdsClient.getClientInstanceId(req),
        statusId,
        analysisDTO
    );
  }

  public void evaluateWithPolling(
      String statusId,
      Application app,
      ClientScanType clientScanType,
      Stage stage,
      ScanTriggerType scanTriggerType,
      File tempScanFile,
      String thirdPartyScanType,
      String clientUserAgent,
      String clientInstanceId)
  {
    evaluateWithPolling(
        statusId,
        app,
        clientScanType,
        stage,
        scanTriggerType,
        tempScanFile,
        thirdPartyScanType,
        clientUserAgent,
        clientInstanceId,
        null
    );
  }

  /**
   * Starts the evaluation of an {@link Application}, type and stage. The passed <code>statusId</code> is passed as
   * reference for the requester to use to check on resultsvia {@link #pollEvaluationResult(String, String)}
   *
   * @param statusId custom unique id, used as a reference for the evaluation done for this request
   * @param app {@link Application}
   * @param clientScanType {@link ClientScanType}
   * @param stage {@link Stage}
   * @param scanTriggerType the type of trigger for the scan for this evaluation {@link ScanTriggerType}
   * @param tempScanFile {@link File} to temporary store scanned result to
   * @param thirdPartyScanType string value of an {@link IntegrationType} if <code>clientScanType</code>
   *          is {@link ClientScanType#SONATYPE_THIRD_PARTY} or null otherwise
   * @param clientUserAgent User agent from {@link HttpServletRequest}
   * @param clientInstanceId Client instance ID {@link HttpServletRequest}
   * @param scanContext any information to be passed down through the scan {@link ScanContext}
   */
  public void evaluateWithPolling(
      String statusId,
      Application app,
      ClientScanType clientScanType,
      Stage stage,
      ScanTriggerType scanTriggerType,
      File tempScanFile,
      String thirdPartyScanType,
      String clientUserAgent,
      String clientInstanceId,
      ScanContext scanContext)
  {
    if (stageTypeService.getLicensedStageTypes().stream()
        .anyMatch(stageType -> stageType.getId().equals(stage.getStageTypeId())))
    {
      PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
          policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultIfNeeded(app.getId(), statusId,
              disablePollingIntervalForTesting);

      log.debug(
          "Submitting policy evaluation task for app public id {}, clientScanType {}, stageTypeId {}. "
              + "The status ID of the operation is {}.",
          app.getPublicId(), clientScanType, stage.getStageTypeId(), statusId);

      TelemetryData thirdPartyScanTelemetryData = telemetryUtils.buildThirdPartyScanTelemetryData(
          app.getPublicId(), stage, thirdPartyScanType, scanTriggerType, clientUserAgent);

      AuditData.get().continueAsync(
          new CompleteEvaluationTask(app, clientScanType, statusId, stage, scanTriggerType, tempScanFile,
              thirdPartyScanTelemetryData, persistedPolicyEvaluationPollingResult, clientUserAgent,
              clientInstanceId, scanContext, null), executor::submit);
    }
    else {
      throw new BadRequestException("Invalid stage: " + stage.getStageTypeId());
    }
  }

  /**
   * Retrieve the {@link PolicyEvaluationPollingResult} for an existing request, made
   * through the {@link #evaluateWithPolling(IntegrationType, String, ClientScanType, HttpServletRequest, Stage)}
   *
   * @param applicationPublicId public shared id
   * @param statusId id from status, normally gotten from {@link PolicyEvaluationReceipt}
   *
   * @since 1.69
   */
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public PolicyEvaluationPollingResultDTO pollEvaluationResult(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      String statusId)
  {
    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResultDAO
            .getByApplicationIdAndStatusId(applicationDAO.getByPublicId(applicationPublicId).getId(), statusId);
    if (persistedPolicyEvaluationPollingResult == null) {
      throw new NotFoundException(String
          .format("Policy evaluation status with id %s for public application id %s was not found.", statusId,
              applicationPublicId));
    }

    return toPolicyEvaluationPollingResultDTO(persistedPolicyEvaluationPollingResult);
  }

  /**
   * Starts the evaluation of an {@link Application}, {@link ScanTriggerType}, and {@link Stage} with polling. After
   * starting, it will return a {@link PolicyEvaluationReceipt} for the requester to use to check on results via
   * {@link #pollEvaluationResult(String, String)}.
   *
   * @param integrationType     the type of integration {@link IntegrationType}
   * @param applicationPublicId the public shared ID of the application
   * @param clientScanType      the type of client scan {@link ClientScanType}
   * @param req                 the HTTP servlet request {@link HttpServletRequest}
   * @param stage               the stage of the evaluation {@link Stage}
   * @param scanTriggerType     the type of trigger for the scan {@link ScanTriggerType}
   * @param clientUserAgent     the user agent from the HTTP request
   * @param clientInstanceId    the client instance ID from the HTTP request
   * @param statusId            the status ID of a previous evaluation
   * @param analysisDTO         the vulnerability signature analysis data transfer object
   *                            {@link VulnerabilitySignatureAnalysisDTO}
   * @return a receipt for the policy evaluation {@link PolicyEvaluationReceipt}
   */
  @VisibleForTesting
  protected PolicyEvaluationReceipt evaluateWithPolling(
      final IntegrationType integrationType,
      final String applicationPublicId,
      final ClientScanType clientScanType,
      final HttpServletRequest req,
      final Stage stage,
      final ScanTriggerType scanTriggerType,
      final String clientUserAgent,
      final String clientInstanceId,
      final String statusId,
      final VulnerabilitySignatureAnalysisDTO analysisDTO)
  {
    EvaluationUtils.ensureNewEvaluationProcessEnabled();

    policyEvaluationUtil.validateEvaluationTypeAndFeature(integrationType, stage);

    log.debug("Received request to evaluate policy, with vulnerability signature analysis, for app public id {}, " +
            "clientScanType {}, stageTypeId {}. The status ID of the operation is {}.",
        applicationPublicId, clientScanType, stage.getStageTypeId(), statusId);

    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    validateLicenseLimits(stage);

    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        findValidComponentAnalysis(app, statusId);

    log.debug("Submitting policy evaluation task, with vulnerability signature analysis, for app public id {}, " +
            "clientScanType {}, stageTypeId {}. The status ID of the operation is {}.",
        app.getPublicId(), clientScanType, stage.getStageTypeId(), statusId);

    AuditData.get().continueAsync(
        new CompleteEvaluationTask(app, clientScanType, statusId, stage, scanTriggerType, null, null,
            persistedPolicyEvaluationPollingResult, clientUserAgent, clientInstanceId, null, analysisDTO
        ), executor::submit
    );

    PolicyEvaluationReceipt policyEvaluationReceipt = new PolicyEvaluationReceipt();
    policyEvaluationReceipt.setStatusId(statusId);
    return policyEvaluationReceipt;
  }

  private void validateLicenseLimits(final Stage stage) {
    if (stageTypeService.getLicensedStageTypes().contains(StageTypes.COMPLIANCE)
        && stage.getStageTypeId().equals(Stage.ID_COMPLIANCE)
        && sbomMetadataUtils.hasMaxSbomLimitBeenReached()) {
      throw new PaymentRequiredException(
          "You have exceeded the licensed limit of " + productLicense.getMaxSboms() + " sboms.");
    }
  }

  private PersistedPolicyEvaluationPollingResult findValidComponentAnalysis(
      final Application application,
      final String componentAnalysisStatusId)
  {
    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(
            application.getId(),
            componentAnalysisStatusId
        );

    if (persistedPolicyEvaluationPollingResult == null) {
      throw new BadRequestException("Component Analysis not found for Application ID: "
          + application.getPublicId() + " and Status ID: " + componentAnalysisStatusId);
    }

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();

    PolicyEvaluationSubStatus subStatus = policyEvaluationPollingResult.getSubStatus();

    if (!COMPONENT_ANALYSIS_COMPLETE.equals(subStatus)) {
      throw new BadRequestException(
          "Component analysis has not completed for public application id: " + application.getPublicId()
              + " and status ID: " + componentAnalysisStatusId
              + " The current status is " + policyEvaluationPollingResult.getStatus()
              + " and the current sub status is " + subStatus);
    }

    return persistedPolicyEvaluationPollingResult;
  }

  /**
   * @since 1.69
   */
  class CompleteEvaluationTask
      extends EvaluationTask
  {
    private final Application app;

    private final ClientScanType clientScanType;

    private final String statusId;

    private final Stage stage;

    private final ScanTriggerType scanTriggerType;

    private final File tempScanFile;

    private final TelemetryData thirdPartyScanTelemetryData;

    private final long taskCreateTime = System.currentTimeMillis();

    private final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult;

    private final String clientUserAgent;

    private final String clientInstanceId;

    private final ScanContext scanContext;

    private final VulnerabilitySignatureAnalysisDTO analysisDTO;

    CompleteEvaluationTask(
        final Application app,
        final ClientScanType clientScanType,
        final String statusId,
        final Stage stage,
        final ScanTriggerType scanTriggerType,
        final File tempScanFile,
        final TelemetryData thirdPartyScanTelemetryData,
        final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult,
        final String clientUserAgent,
        final String clientInstanceId,
        final ScanContext scanContext,
        final VulnerabilitySignatureAnalysisDTO analysisDTO)
    {
      this.app = app;
      this.clientScanType = clientScanType;
      this.statusId = statusId;
      this.stage = stage;
      this.scanTriggerType = scanTriggerType;
      this.tempScanFile = tempScanFile;
      this.thirdPartyScanTelemetryData = thirdPartyScanTelemetryData;
      this.persistedPolicyEvaluationPollingResult = persistedPolicyEvaluationPollingResult;
      this.clientUserAgent = clientUserAgent;
      this.clientInstanceId = clientInstanceId;
      this.scanContext = scanContext;
      this.analysisDTO = analysisDTO;
    }

    @Override
    public void run() {
      log.debug(
          "Policy evaluation task (appPublicId {}, stageTypeId {}, statusId {}) waited in queue for {} ms.",
          app.getPublicId(), stage.getStageTypeId(), statusId, System.currentTimeMillis() - taskCreateTime);

      Sample sample = policyEvaluateServiceMetrics.emitStartPolicyEvaluation();

      PolicyEvaluationPollingResult policyEvaluationPollingResult =
          persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();

      try {
        final long start = System.currentTimeMillis();

        if (shouldContinueExistingEvaluation()) {
          policyEvaluationPollingResult = continueEvaluation(policyEvaluationPollingResult);
        }
        else {
          policyEvaluationPollingResult = scanAndEvaluate(policyEvaluationPollingResult);
        }

        log.debug(
            "Evaluated policy for app public id {}, scan id {}, stageTypeId {} in {} ms."
                + " The status ID of the operation is {}.",
            app.getPublicId(), getScanId(policyEvaluationPollingResult), stage.getStageTypeId(),
            System.currentTimeMillis() - start, statusId
        );
      }
      catch (Exception e) {
        log.error(
            "Failed to evaluate policy for app public id {}, scan id {}, stageTypeId {}."
                + " The status ID of the operation is {}.",
            app.getPublicId(), getScanId(policyEvaluationPollingResult), stage.getStageTypeId(), statusId, e
        );

        policyEvaluationPollingResult = failEvaluation(e, policyEvaluationPollingResult);
      }

      updatePolicyEvaluationPollingResult(policyEvaluationPollingResult);
      policyEvaluateServiceMetrics.emitEndPolicyEvaluation(sample);
    }

    /**
     * Check if the evaluation should continue based on the {@link PolicyEvaluationPollingResult#getSubStatus()} being
     * {@link PolicyEvaluationSubStatus#COMPONENT_ANALYSIS_COMPLETE}. When this condition is met, it means an evaluation
     * was started as a Component Analysis and should continue here as a policy evaluation.
     *
     * @return {@code true} if the evaluation should continue, {@code false} otherwise
     */
    private boolean shouldContinueExistingEvaluation() {
      return COMPONENT_ANALYSIS_COMPLETE.equals(
          persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult().getSubStatus()
      );
    }

    /**
     * Continue the evaluation of an application with a given {@link PolicyEvaluationPollingResult} containing already
     * an evaluated component scan. Using a scan receipt of a previously run {@link ComponentAnalysisService}.
     *
     * @return a new {@link PolicyEvaluationPollingResult} with the evaluation continued
     * @throws IOException if the associated report file is unable to be read or processed
     */
    private PolicyEvaluationPollingResult continueEvaluation(
        final PolicyEvaluationPollingResult policyEvaluationPollingResult
    ) throws IOException
    {
      policyEvaluationPollingResult.setSubStatus(PolicyEvaluationSubStatus.POLICY_EVALUATION_PENDING);
      updatePolicyEvaluationPollingResult(policyEvaluationPollingResult);

      ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(
          app, policyEvaluationPollingResult.getScanReceipt().getScanId(),
          stage, scanTriggerType, clientUserAgent, clientInstanceId, clientScanType, analysisDTO
      );

      if (!results.evaluation.isReevaluation()) {
        policyAlertNotifier.sendNotifications(app, results);
      }

      PolicyEvaluationResult policyEvaluationResult = scanPolicyEvaluator
          .createPolicyEvaluationResult(results.evaluation, results.allViolations, true);

      PolicyEvaluationPollingResult result = makeCopy(policyEvaluationPollingResult);
      result.setStatus(PolicyEvaluationStatus.COMPLETED);
      result.setSubStatus(PolicyEvaluationSubStatus.POLICY_EVALUATION_COMPLETE);
      result.setResult(policyEvaluationResult);
      return result;
    }

    /**
     * Default behavior for scanning and evaluating an application.
     *
     * @return a new {@link PolicyEvaluationPollingResult} with the evaluation completed
     * @throws IOException if the associated report file is unable to be read or processed
     */
    private PolicyEvaluationPollingResult scanAndEvaluate(
        final PolicyEvaluationPollingResult policyEvaluationPollingResult
    ) throws IOException
    {
      ScanReceipt scanReceipt =
          scanHandler.handle(tempScanFile, app, clientScanType, thirdPartyScanTelemetryData, stage.getStageTypeId(),
              clientUserAgent, persistedPolicyEvaluationPollingResult.getStatusId(), scanContext);

      String scanId = scanReceipt.getScanId();

      policyEvaluationPollingResult.setScanReceipt(scanReceipt);
      updatePolicyEvaluationPollingResult(policyEvaluationPollingResult);

      log.debug(
          "Evaluating policy for app public id {}, scan id {}, stageTypeId {}. The status ID of the operation is {}.",
          app.getPublicId(), scanId, stage.getStageTypeId(), statusId
      );

      PolicyEvaluationResult policyEvaluationResult = evaluate(
          app, scanId, stage, scanTriggerType, clientUserAgent, clientInstanceId, clientScanType
      );

      PolicyEvaluationPollingResult result = new PolicyEvaluationPollingResult();
      result.setScanReceipt(scanReceipt);
      result.setResult(policyEvaluationResult);
      result.setStatus(PolicyEvaluationStatus.COMPLETED);
      return result;
    }

    private PolicyEvaluationPollingResult failEvaluation(
        final Exception e,
        final PolicyEvaluationPollingResult policyEvaluationPollingResult)
    {
      // in failed status, hold onto as much as we have obtained so far
      PolicyEvaluationPollingResult failedEvaluationPollingResult = makeCopy(policyEvaluationPollingResult);
      failedEvaluationPollingResult.setStatus(PolicyEvaluationStatus.FAILED);
      failedEvaluationPollingResult.setReason(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody());
      AuditData.get().setException(
          new RuntimeException(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody(), e)
      );
      return failedEvaluationPollingResult;
    }

    private void updatePolicyEvaluationPollingResult(
        final PolicyEvaluationPollingResult policyEvaluationPollingResult)
    {
      persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
      persistedPolicyEvaluationPollingResultDAO.update(persistedPolicyEvaluationPollingResult);
    }

    private String getScanId(final PolicyEvaluationPollingResult policyEvaluationPollingResult) {
      if (policyEvaluationPollingResult != null && policyEvaluationPollingResult.getScanReceipt() != null) {
        return policyEvaluationPollingResult.getScanReceipt().getScanId();
      }
      return null;
    }
  }

  /**
   * Evaluate an application using the given scan file
   */
  public PolicyEvaluation evaluateSynchronousNoAuth(
      Application application,
      ClientScanType clientScanType,
      File scanFile,
      Stage stage,
      ScanTriggerType scanTriggerType,
      String clientUserAgent) throws IOException
  {
    log.debug("Received request to evaluate policy for app public id {}, stageTypeId {}",
        application.getPublicId(), stage.getStageTypeId());

    String scanId = null;

    try {
      TelemetryData thirdPartyScanTelemetryData =
          telemetryUtils.buildThirdPartyScanTelemetryData(application.getPublicId(), stage,
              null /* thirdPartyScanType */, scanTriggerType, clientUserAgent);
      ScanReceipt scanReceipt = scanHandler.handle(scanFile, application, clientScanType, thirdPartyScanTelemetryData,
          stage.getStageTypeId(), clientUserAgent);
      scanId = scanReceipt.getScanId();

      log.debug("Evaluating policy for app public id {}, scan id {}, stageTypeId {}.", application.getPublicId(),
          scanId, stage.getStageTypeId());

      long start = System.currentTimeMillis();
      ScanPolicyEvaluatorResults results =
          evaluateAndSendNotifications(application, scanId, stage, scanTriggerType, clientUserAgent, null,
              clientScanType);

      log.debug("Evaluated policy for app public id {}, scan id {}, stageTypeId {} in {} ms.",
          application.getPublicId(), scanId, stage.getStageTypeId(), System.currentTimeMillis() - start);

      return results.evaluation;
    }
    catch (Exception e) {
      log.error("Failed to evaluate policy for app public id {}, scan id {}, stageTypeId {}.",
          application.getPublicId(), scanId, stage.getStageTypeId());
      AuditData.get()
          .setException(new RuntimeException(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody(), e));
      throw e;
    }
  }

  /**
   * Make a DTO of the given {@link PolicyEvaluationPollingResult} instance with a potential sub status
   * ({@link PolicyEvaluationSubStatus}), if the feature {@link SystemConfigurationPropertyFeature#NEW_SCAN_PROCESS} is
   * enabled.
   *
   * @param persistedPolicyEvaluationPollingResult the {@link PolicyEvaluationPollingResult} to create a DTO from.
   * @return a new instance of {@link PolicyEvaluationPollingResultDTO}
   */
  @VisibleForTesting
  protected PolicyEvaluationPollingResultDTO toPolicyEvaluationPollingResultDTO(
      final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult)
  {
    PolicyEvaluationPollingResult res = persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();

    PolicyEvaluationPollingResultDTO dto = new PolicyEvaluationPollingResultDTO();
    dto.status = res.getStatus();
    dto.result = res.getResult();
    dto.reason = res.getReason();
    dto.scanReceipt = res.getScanReceipt();
    dto.nextPollingIntervalInSeconds = res.getNextPollingIntervalInSeconds();
    dto.statusId = persistedPolicyEvaluationPollingResult.getStatusId();

    Set<Feature> features = featuresService.getFeatures();
    if (features.contains(SystemConfigurationPropertyFeature.NEW_SCAN_PROCESS)) {
      dto.subStatus = res.getSubStatus();
    }

    return dto;
  }
}
