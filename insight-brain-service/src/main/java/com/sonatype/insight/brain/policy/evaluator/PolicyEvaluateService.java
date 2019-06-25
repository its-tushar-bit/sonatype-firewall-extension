/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.features.Feature;
import com.sonatype.insight.brain.hds.ScanHandler;
import com.sonatype.insight.brain.integration.IntegrationType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class PolicyEvaluateService
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluateService.class);

  private static final int NEXT_POLLING_INTERVAL_IN_SECONDS = 5;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private final ErrorResponseGenerator errorResponseGenerator;

  private final ThreadPoolExecutor executor;

  private final ScanHandler scanHandler;

  private final CLMLicenseManager clmLicenseManager;

  @VisibleForTesting
  final Cache<String, Future<PolicyEvaluationPollingResult>> policyEvaluationPollingResults =
      CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.HOURS)
          .build();

  @Inject
  public PolicyEvaluateService(ScanPolicyEvaluator scanPolicyEvaluator,
                               PolicyAlertNotifier policyAlertNotifier,
                               ErrorResponseGenerator errorResponseGenerator,
                               ScanHandler scanHandler,
                               CLMLicenseManager clmLicenseManager)
  {
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.errorResponseGenerator = errorResponseGenerator;
    this.scanHandler = scanHandler;
    this.clmLicenseManager = clmLicenseManager;

    executor = new ThreadPoolExecutor(5, 100, 5L, TimeUnit.MINUTES,
        new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat("PolicyEvaluateService-%d").build());
    executor.allowCoreThreadTimeOut(true);
  }

  private String getPolicyEvaluationKey(String applicationId, String statusId) {
    return applicationId + ":" + statusId;
  }

  // default access for testing
  PolicyEvaluationResult doPolicyEvaluation(String applicationPublicId, String scanId, Stage stage)
      throws IOException
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    ScanPolicyEvaluatorResults results = scanPolicyEvaluator.evaluate(application, scanId, stage);
    PolicyEvaluationResult policyEvaluationResult = scanPolicyEvaluator.createPolicyEvaluationResult(results.evaluation,
        results.allViolations, true);

    if (!results.evaluation.isReevaluation()) {
      policyAlertNotifier.sendNotifications(application, results);
    }

    return policyEvaluationResult;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION, anonymousAllowed = true)
  public PolicyEvaluationResult evaluate(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      String scanId,
      Stage stage) throws IOException
  {
    log.debug("Received request to evaluate policy for app public id {}, scan id {}, stageTypeId {}",
        applicationPublicId, scanId, stage.getStageTypeId());

    return doPolicyEvaluation(applicationPublicId, scanId, stage);
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public PolicyEvaluationReceipt evaluateWithPolling(
      IntegrationType integrationType,
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      ClientScanType clientScanType,
      HttpServletRequest req,
      Stage stage) throws IOException
  {
    if (integrationType.equals(IntegrationType.CLI) && !clmLicenseManager.hasFeature(Feature.CLI_INTEGRATION)) {
      throw new InvalidLicenseException();
    }
    else if (integrationType.equals(IntegrationType.CI) && !clmLicenseManager.hasFeature(Feature.CI_INTEGRATION)) {
      throw new InvalidLicenseException();
    }
    else if (integrationType.equals(IntegrationType.RM) &&
        !clmLicenseManager.hasFeature(Feature.RM_STAGING_INTEGRATION)) {
      throw new InvalidLicenseException();
    }

    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new InvalidStageException("Invalid stage id=" + stage.getStageTypeId());
    }

    String statusId = UUID.randomUUID().toString().replace("-", "");
    log.debug(
        "Received request to evaluate policy for app public id {}, clientScanType{}, stageTypeId {}. " +
            "The status ID of the operation is {}.",
        applicationPublicId, clientScanType, stage.getStageTypeId(), statusId);

    File tempScanFile = scanHandler.createTempScanFile(req, applicationPublicId, clientScanType);
    policyEvaluationPollingResults.put(getPolicyEvaluationKey(applicationPublicId, statusId), AuditData.get()
        .continueAsync(new EvaluationTask(applicationPublicId, clientScanType, statusId, stage, tempScanFile),
            executor::submit));

    PolicyEvaluationReceipt policyEvaluationReceipt = new PolicyEvaluationReceipt();
    policyEvaluationReceipt.setStatusId(statusId);

    return policyEvaluationReceipt;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public PolicyEvaluationPollingResult pollEvaluationResult(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      String statusId)
  {
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setNextPollingIntervalInSeconds(NEXT_POLLING_INTERVAL_IN_SECONDS);
    Future<PolicyEvaluationPollingResult> evaluationPollingResultFuture =
        policyEvaluationPollingResults.getIfPresent(getPolicyEvaluationKey(applicationPublicId, statusId));
    if (evaluationPollingResultFuture == null) {
      throw new NotFoundException(String
          .format("Policy evaluation status with id %s for public application id %s was not found.", statusId,
              applicationPublicId));
    }
    if (!evaluationPollingResultFuture.isDone()) {
      policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.PENDING);
      return policyEvaluationPollingResult;
    }

    try {
      policyEvaluationPollingResult.setResult(evaluationPollingResultFuture.get().getResult());
      policyEvaluationPollingResult.setScanReceipt(evaluationPollingResultFuture.get().getScanReceipt());
    }
    catch (Exception e) {
      policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.FAILED);
      policyEvaluationPollingResult.setReason(e.getCause().getMessage());
      return policyEvaluationPollingResult;
    }
    policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.COMPLETED);
    return policyEvaluationPollingResult;
  }

  class EvaluationTask
      implements Callable<PolicyEvaluationPollingResult>
  {
    private final String applicationPublicId;

    private final ClientScanType clientScanType;

    private final String statusId;

    private final Stage stage;

    private final File tempScanFile;

    EvaluationTask(final String applicationPublicId,
                   final ClientScanType clientScanType,
                   final String statusId,
                   final Stage stage,
                   final File tempScanFile)
    {
      this.applicationPublicId = applicationPublicId;
      this.clientScanType = clientScanType;
      this.statusId = statusId;
      this.stage = stage;
      this.tempScanFile = tempScanFile;
    }

    @Override
    public PolicyEvaluationPollingResult call() {
      String scanId = null;
      try {
        PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
        ScanReceipt scanReceipt = scanHandler.handle(tempScanFile, applicationPublicId, clientScanType);
        scanId = scanReceipt.getScanId();

        final long start = System.currentTimeMillis();

        log.debug(
            "Evaluating policy for app public id {}, scan id {}, stageTypeId {}. The status ID of the operation is {}.",
            applicationPublicId, scanId, stage.getStageTypeId(), statusId);

        PolicyEvaluationResult policyEvaluationResult =
            doPolicyEvaluation(applicationPublicId, scanId, stage);

        log.debug(
            "Evaluating policy for app public id {}, scan id {}, stageTypeId {} in {} ms." +
                " The status ID of the operation is {}.",
            applicationPublicId, scanId, stage.getStageTypeId(), System.currentTimeMillis() - start, statusId);

        policyEvaluationPollingResult.setResult(policyEvaluationResult);
        policyEvaluationPollingResult.setScanReceipt(scanReceipt);
        return policyEvaluationPollingResult;
      }
      catch (Exception e) {
        log.error(
            "Failed to evaluate policy for app public id {}, scan id {}, stageTypeId {}." +
                " The status ID of the operation is {}.",
            applicationPublicId, scanId, stage.getStageTypeId(), statusId);
        throw new RuntimeException(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody(), e);
      }
    }
  }
}
