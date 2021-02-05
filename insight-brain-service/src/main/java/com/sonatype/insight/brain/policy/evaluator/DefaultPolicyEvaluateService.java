/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.hds.DefaultHdsClient;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluationTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.telemetry.TelemetryUtils.buildThirdPartyScanTelemetryData;

@Named
@Singleton
public class DefaultPolicyEvaluateService
    extends AbstractPolicyEvaluateService
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(DefaultPolicyEvaluateService.class);

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private final ErrorResponseGenerator errorResponseGenerator;

  private final PolicyEvaluationThreadPoolExecutor executor;

  private final ScanHandler scanHandler;

  private final ProductLicense productLicense;

  private final StageTypeService stageTypeService;

  private final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  private final InsightWork insightWork;

  @Inject
  public DefaultPolicyEvaluateService(
      ScanPolicyEvaluator scanPolicyEvaluator,
      PolicyAlertNotifier policyAlertNotifier,
      ErrorResponseGenerator errorResponseGenerator,
      ScanHandler scanHandler,
      ProductLicense productLicense,
      StageTypeService stageTypeService,
      PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO,
      InsightWork insightWork)
  {
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.errorResponseGenerator = errorResponseGenerator;
    this.scanHandler = scanHandler;
    this.productLicense = productLicense;
    this.stageTypeService = stageTypeService;
    this.persistedPolicyEvaluationPollingResultDAO = persistedPolicyEvaluationPollingResultDAO;
    this.insightWork = insightWork;

    executor = new PolicyEvaluationThreadPoolExecutor();
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
      PolicyEvaluationTriggerType policyEvaluationTriggerType)
      throws IOException
  {
    ScanPolicyEvaluatorResults results =
        scanPolicyEvaluator.evaluate(application, scanId, stage, policyEvaluationTriggerType);
    PolicyEvaluationResult policyEvaluationResult = scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation,
        results.allViolations, true);

    if (!results.evaluation.isReevaluation()) {
      policyAlertNotifier.sendNotifications(application, results);
    }

    return policyEvaluationResult;
  }

  @Override
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public PolicyEvaluationResult evaluate(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String scanId,
      Stage stage,
      PolicyEvaluationTriggerType policyEvaluationTriggerType) throws IOException
  {
    log.debug("Received request to evaluate policy for app public id {}, scan id {}, stageTypeId {}",
        applicationPublicId, scanId, stage.getStageTypeId());

    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    File scanFile = insightWork.getScanFile(app.getId(), scanId);
    if (!scanFile.exists()) {
      throw new NotFoundException("Cannot find scan with ID " + scanId);
    }

    return evaluate(app, scanId, stage, policyEvaluationTriggerType);
  }

  /**
   * @since 1.69
   */
  @Override
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public PolicyEvaluationReceipt evaluateWithPolling(
      IntegrationType integrationType,
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ClientScanType clientScanType,
      HttpServletRequest req,
      Stage stage) throws IOException
  {
    if (integrationType.equals(IntegrationType.CLI)) {
      productLicense.validateFeature(LicensedFeature.CLI_INTEGRATION);
    }
    else if (integrationType.equals(IntegrationType.CI)) {
      productLicense.validateFeature(LicensedFeature.CI_INTEGRATION);
    }
    else if (integrationType.equals(IntegrationType.RM)) {
      productLicense.validateFeature(LicensedFeature.RM_STAGING_INTEGRATION);
    }

    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new InvalidStageException(stage.getStageTypeId());
    }

    if (!stageTypeService.getLicensedStageTypes().contains(StageTypes.getById(stage.getStageTypeId()))) {
      throw new InvalidLicenseException("Stage '" + stage.getStageTypeId() + "' is not supported by your license.");
    }

    String statusId = UUID.randomUUID().toString().replace("-", "");
    log.debug(
        "Received request to evaluate policy for app public id {}, clientScanType {}, stageTypeId {}. " +
            "The status ID of the operation is {}.",
        applicationPublicId, clientScanType, stage.getStageTypeId(), statusId);

    Application app = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    File tempScanFile = scanHandler.createTempScanFile(req, app);

    String thirdPartyScanType =
        clientScanType == ClientScanType.SONATYPE_THIRD_PARTY ? integrationType.toString() : null;

    evaluateWithPolling(statusId, app, clientScanType, stage, getPolicyEvaluationTriggerType(integrationType),
        tempScanFile, thirdPartyScanType, DefaultHdsClient.getClientUserAgent(req));

    PolicyEvaluationReceipt policyEvaluationReceipt = new PolicyEvaluationReceipt();
    policyEvaluationReceipt.setStatusId(statusId);

    return policyEvaluationReceipt;
  }

  private PolicyEvaluationTriggerType getPolicyEvaluationTriggerType(IntegrationType integrationType) {
    switch (integrationType) {
      case CI:
        return PolicyEvaluationTriggerType.CONTINUOUS_INTEGRATION;
      case CLI:
        return PolicyEvaluationTriggerType.CLI;
      case RM:
        return PolicyEvaluationTriggerType.REPOSITORY_MANAGER;
      default:
        throw new IllegalArgumentException("Unknown integration type " + integrationType);
    }
  }

  @Override
  public void evaluateWithPolling(
      String statusId,
      Application app,
      ClientScanType clientScanType,
      Stage stage,
      PolicyEvaluationTriggerType policyEvaluationTriggerType,
      File tempScanFile,
      String thirdPartyScanType,
      String clientUserAgent)
  {
    // to avoid any race condition when the following task attempts to update
    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        createPersistedPolicyEvaluationPollingResultIfNeeded(app, statusId);

    log.debug(
        "Submitting policy evaluation task for app public id {}, clientScanType {}, stageTypeId {}. "
            + "The status ID of the operation is {}.",
        app.getPublicId(), clientScanType, stage.getStageTypeId(), statusId);
    TelemetryData thirdPartyTelemetryData =
        buildThirdPartyScanTelemetryData(app.getPublicId(), stage, thirdPartyScanType, clientUserAgent);
    AuditData.get().continueAsync(
        new Task(app, clientScanType, statusId, stage, policyEvaluationTriggerType, tempScanFile,
            thirdPartyTelemetryData, persistedPolicyEvaluationPollingResult, clientUserAgent),
        executor::submit);
  }

  public PersistedPolicyEvaluationPollingResult createPersistedPolicyEvaluationPollingResultIfNeeded(
      Application app,
      String statusId)
  {
    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(app.getId(), statusId);
    if (persistedPolicyEvaluationPollingResult != null) {
      return persistedPolicyEvaluationPollingResult;
    }

    PolicyEvaluationPollingResult initialResult = new PolicyEvaluationPollingResult();
    initialResult.setStatus(PolicyEvaluationStatus.PENDING);
    initialResult.setNextPollingIntervalInSeconds(getNextPollingInterval());
    persistedPolicyEvaluationPollingResult =
        new PersistedPolicyEvaluationPollingResult(app.getId(), statusId, initialResult);
    persistedPolicyEvaluationPollingResultDAO.insert(persistedPolicyEvaluationPollingResult);

    return persistedPolicyEvaluationPollingResult;
  }

  /**
   * @since 1.69
   */
  @Override
  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public PolicyEvaluationPollingResult pollEvaluationResult(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      String statusId)
  {
    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResultDAO
            .getByApplicationIdAndStatusId(new ApplicationDAO().getByPublicId(applicationPublicId).getId(), statusId);
    if (persistedPolicyEvaluationPollingResult == null) {
      throw new NotFoundException(String
          .format("Policy evaluation status with id %s for public application id %s was not found.", statusId,
              applicationPublicId));
    }
    return persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();
  }

  /**
   * @since 1.69
   */
  class Task
      extends EvaluationTask
  {
    private final Application app;

    private final ClientScanType clientScanType;

    private final String statusId;

    private final Stage stage;

    private final PolicyEvaluationTriggerType policyEvaluationTriggerType;

    private final File tempScanFile;

    private final TelemetryData telemetryData;

    private final long taskCreateTime = System.currentTimeMillis();

    private final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult;

    private final String clientUserAgent;

    Task(
        final Application app,
        final ClientScanType clientScanType,
        final String statusId,
        final Stage stage,
        final PolicyEvaluationTriggerType policyEvaluationTriggerType,
        final File tempScanFile,
        final TelemetryData telemetryData,
        final PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult,
        final String clientUserAgent)
    {
      this.app = app;
      this.clientScanType = clientScanType;
      this.statusId = statusId;
      this.stage = stage;
      this.policyEvaluationTriggerType = policyEvaluationTriggerType;
      this.tempScanFile = tempScanFile;
      this.telemetryData = telemetryData;
      this.persistedPolicyEvaluationPollingResult = persistedPolicyEvaluationPollingResult;
      this.clientUserAgent = clientUserAgent;
    }

    @Override
    public void run() {
      log.debug(
          "Policy evaluation task (appPublicId {}, stageTypeId {}, statusId {}) waited in queue for {} ms.",
          app.getPublicId(), stage.getStageTypeId(), statusId, System.currentTimeMillis() - taskCreateTime);

      String scanId = null;
      PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
      policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.PENDING);
      policyEvaluationPollingResult.setNextPollingIntervalInSeconds(getNextPollingInterval());

      try {
        ScanReceipt scanReceipt = scanHandler.handle(tempScanFile, app, clientScanType, telemetryData,
            stage.getStageTypeId(), clientUserAgent);
        scanId = scanReceipt.getScanId();

        policyEvaluationPollingResult.setScanReceipt(scanReceipt);
        persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
        persistedPolicyEvaluationPollingResultDAO.update(persistedPolicyEvaluationPollingResult);

        final long start = System.currentTimeMillis();

        log.debug(
            "Evaluating policy for app public id {}, scan id {}, stageTypeId {}. The status ID of the operation is {}.",
            app.getPublicId(), scanId, stage.getStageTypeId(), statusId);

        PolicyEvaluationResult policyEvaluationResult = evaluate(app, scanId, stage, policyEvaluationTriggerType);

        log.debug(
            "Evaluated policy for app public id {}, scan id {}, stageTypeId {} in {} ms."
                + " The status ID of the operation is {}.",
            app.getPublicId(), scanId, stage.getStageTypeId(), System.currentTimeMillis() - start, statusId);

        policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
        policyEvaluationPollingResult.setScanReceipt(scanReceipt);
        policyEvaluationPollingResult.setResult(policyEvaluationResult);
        policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.COMPLETED);
      }
      catch (Exception e) {
        log.error(
            "Failed to evaluate policy for app public id {}, scan id {}, stageTypeId {}." +
                " The status ID of the operation is {}.",
            app.getPublicId(), scanId, stage.getStageTypeId(), statusId);
        // in failed status, hold onto as much as we have obtained so far
        policyEvaluationPollingResult = makeCopy(policyEvaluationPollingResult);
        policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.FAILED);
        policyEvaluationPollingResult.setReason(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody());
        AuditData.get()
            .setException(new RuntimeException(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody(), e));
      }
      persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
      persistedPolicyEvaluationPollingResultDAO.update(persistedPolicyEvaluationPollingResult);
    }
  }
}
