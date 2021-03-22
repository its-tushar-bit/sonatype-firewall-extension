/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.DefaultPolicyEvaluateService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.51.0
 */
@Named
@Singleton
public class ApiPromoteScanServiceV2
    extends AbstractApiApplicationEvaluationService
{
  private static final Logger log = LoggerFactory.getLogger(ApiPromoteScanServiceV2.class);

  private final ThreadPoolExecutor executor;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  private final InsightWork work;
  
  private final ErrorResponseGenerator errorResponseGenerator;

  @Inject
  public ApiPromoteScanServiceV2(
      ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO,
      DefaultPolicyEvaluateService policyEvaluateService,
      InsightWork work,
      ErrorResponseGenerator errorResponseGenerator)
  {
    super(applicationDAO, policyEvaluateService);
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.persistedPolicyEvaluationPollingResultDAO = persistedPolicyEvaluationPollingResultDAO;
    this.work = work;
    this.errorResponseGenerator = errorResponseGenerator;

    executor = new ThreadPoolExecutor(100, 100, 5L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat("ApiPromoteScanServiceV2-%d").build());
    executor.allowCoreThreadTimeOut(true);
  }

  private void validateRequest(final ApiPromoteScanRequestDTOV2 requestDTO, final String applicationId) {
    if (requestDTO == null) {
      throw new BadRequestException("Missing parameters.");
    }
    if (requestDTO.scanId == null && requestDTO.sourceStageId == null) {
      throw new BadRequestException("Either scanId or sourceStageId need to be supplied.");
    }
    if (requestDTO.scanId != null && requestDTO.sourceStageId != null) {
      throw new BadRequestException("Only one of scanId or sourceStageId can be supplied.");
    }

    if (!isValidTargetStage(requestDTO.targetStageId)) {
      throw new BadRequestException("Stage " + requestDTO.targetStageId + " is invalid.");
    }

    if (requestDTO.scanId != null) {
      final File scanFile = work.getScanFile(applicationId, requestDTO.scanId);
      if (!scanFile.isFile()) {
        throw new BadRequestException("A scan with ID " + requestDTO.scanId +
            " does not exist on the server and may be obsolete. Note that only the most recent scan for the given" +
            " stage can be promoted.");
      }
    }
    else {
      PolicyEvaluation lastEvaluation = getLastEvaluation(applicationId, requestDTO.sourceStageId);
      if (lastEvaluation == null) {
        throw new BadRequestException("No scan available to promote from stage " + requestDTO.sourceStageId + ".");
      }
      // given scan files get deleted upon new policy evaluation, don't validate its existence here
    }
  }

  private PolicyEvaluation getLastEvaluation(String applicationId, String stageId) {
    return policyEvaluationDAO.getLastByApplicationIdAndStageId(applicationId, stageId);
  }

  private boolean isValidTargetStage(String stageId) {
    return Stage.isValidStageTypeId(stageId) && !Stage.ID_DEVELOP.equals(stageId);
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiApplicationEvaluationStatusDTOV2 promoteScan(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final ApiPromoteScanRequestDTOV2 apiPromoteScanRequestDTOV2,
      String userAgent)
  {
    final Application application = applicationDAO.getByIdNotNull(applicationId);

    validateRequest(apiPromoteScanRequestDTOV2, application.getId());

    String statusId = UUID.randomUUID().toString().replace("-", "");
    policyEvaluateService.createPersistedPolicyEvaluationPollingResultIfNeeded(application, statusId);
    log.debug("Received request to promote scan {} of app {} to stage {}. The status ID of the operation is {}.",
        apiPromoteScanRequestDTOV2.scanId != null ? apiPromoteScanRequestDTOV2.scanId
            : "from stage " + apiPromoteScanRequestDTOV2.sourceStageId,
        application.getName(), apiPromoteScanRequestDTOV2.targetStageId, statusId);

    AuditData.get().continueAsync(executor,
        new ScanPromotionTask(apiPromoteScanRequestDTOV2, application.getId(), statusId, userAgent));

    ApiApplicationEvaluationStatusDTOV2 result = new ApiApplicationEvaluationStatusDTOV2();
    result.statusUrl = getStatusUrl(applicationId, statusId);
    return result;
  }

  class ScanPromotionTask
      implements Runnable
  {
    private final ApiPromoteScanRequestDTOV2 apiPromoteScanRequestDTOV2;

    private final String applicationId;

    private final String statusId;

    private final String userAgent;

    ScanPromotionTask(
        final ApiPromoteScanRequestDTOV2 apiPromoteScanRequestDTOV2,
        final String applicationId,
        final String statusId,
        String userAgent)
    {
      this.apiPromoteScanRequestDTOV2 = apiPromoteScanRequestDTOV2;
      this.applicationId = applicationId;
      this.statusId = statusId;
      this.userAgent = userAgent;
    }

    @Override
    public void run() {
      File tempScanFile = null;
      final String targetStageId = apiPromoteScanRequestDTOV2.targetStageId;
      try {
        final Application application = applicationDAO.getByIdNotNull(applicationId);
        log.debug("Promoting scan {} of app {} to stage {}. The status ID of the operation is {}.",
            apiPromoteScanRequestDTOV2.scanId != null ? apiPromoteScanRequestDTOV2.scanId
                : "from stage " + apiPromoteScanRequestDTOV2.sourceStageId,
            application.getName(), targetStageId, statusId);

        tempScanFile = work.getScanFile(applicationId, "tmp-" + statusId);
        String sourceScanId = getSourceScanId();
        while (true) {
          File sourceScanFile = work.getScanFile(application.getId(), sourceScanId);
          try {
            Files.copy(sourceScanFile.toPath(), tempScanFile.toPath());
            break;
          }
          catch (IOException e) {
            // each new policy evaluation deletes the scan for the previous one in that stage
            // if we find ourselves trying to promote the latest scan from a stage that just got re-evaluated,
            // try again with the new latest scan
            String scanId = getSourceScanId();
            if (!sourceScanId.equals(scanId)) {
              sourceScanId = scanId;
              continue;
            }
            throw e;
          }
        }

        ScanTriggerType scanTriggerType = getScanTriggerType(sourceScanId);
        policyEvaluateService.evaluateWithPolling(statusId, application, ClientScanType.SONATYPE,
            new Stage(targetStageId), scanTriggerType, tempScanFile, "api", userAgent);
      }
      catch (Exception e) {
        log.error("Failed to promote scan of app {} to stage {}. The status ID of the operation is {}.", applicationId,
            targetStageId, statusId);
        String errorMessage = errorResponseGenerator.mapExceptionAndLog(e).getMessageBody();

        PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
            persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(applicationId, statusId);
        PolicyEvaluationPollingResult policyEvaluationPollingResult =
            persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();
        policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.FAILED);
        policyEvaluationPollingResult.setReason(errorMessage);
        persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
        persistedPolicyEvaluationPollingResultDAO.update(persistedPolicyEvaluationPollingResult);

        throw new RuntimeException(errorMessage, e);
      }
    }

    private ScanTriggerType getScanTriggerType(String scanId) {
      PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(applicationId, scanId);
      if (policyEvaluation != null) {
        return policyEvaluation.getScanTriggerType();
      }
      return ScanTriggerType.UNKNOWN;
    }

    private String getSourceScanId() {
      if (apiPromoteScanRequestDTOV2.scanId != null) {
        return apiPromoteScanRequestDTOV2.scanId;
      }
      return getLastEvaluation(applicationId, apiPromoteScanRequestDTOV2.sourceStageId).getScanId();
    }
  }
}
