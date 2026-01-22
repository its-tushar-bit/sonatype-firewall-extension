/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationPollingResultUtils;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluationUtil;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
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

  private final PolicyEvaluationPollingResultUtils policyEvaluationPollingResultUtils;

  private final PolicyEvaluationUtil policyEvaluationUtil;

  private final ScanPersistenceService scanPersistenceService;

  @Inject
  public ApiPromoteScanServiceV2(
      ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      PolicyEvaluateService policyEvaluateService,
      PolicyEvaluationPollingResultUtils policyEvaluationPollingResultUtils,
      ShutdownHandler shutdownHandler,
      PolicyEvaluationUtil policyEvaluationUtil,
      ScanPersistenceService scanPersistenceService)
  {
    super(applicationDAO, policyEvaluateService);
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyEvaluationPollingResultUtils = policyEvaluationPollingResultUtils;
    this.policyEvaluationUtil = policyEvaluationUtil;
    this.scanPersistenceService = scanPersistenceService;

    executor = new TenantThreadPoolExecutor(100, 100, 5L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat("ApiPromoteScanServiceV2-%d").build(), new AbortPolicy(),
        "scan_promotion", "ApiPromoteScanServiceV2");
    executor.allowCoreThreadTimeOut(true);
    shutdownHandler.add(executor);
  }

  // Visible for testing
  ThreadPoolExecutor getExecutor() {
    return executor;
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
      final ScanEntity scanEntity = scanPersistenceService.getScan(applicationId, requestDTO.scanId);
      if (!scanEntity.exists()) {
        throw new BadRequestException("A scan with ID " + requestDTO.scanId +
            " does not exist on the server and may be obsolete. Note that only the most recent scan for the given" +
            " stage can be promoted by default. Set configuration purgeScanFiles to withReports to retain older" +
            " scan files.");
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
    policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultIfNeeded(applicationId, statusId);
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
      ScanEntity tempScanEntity;
      final String targetStageId = apiPromoteScanRequestDTOV2.targetStageId;
      try {
        final Application application = applicationDAO.getByIdNotNull(applicationId);
        log.debug("Promoting scan {} of app {} to stage {}. The status ID of the operation is {}.",
            apiPromoteScanRequestDTOV2.scanId != null ? apiPromoteScanRequestDTOV2.scanId
                : "from stage " + apiPromoteScanRequestDTOV2.sourceStageId,
            application.getName(), targetStageId, statusId);

        tempScanEntity = scanPersistenceService.getScan(applicationId, "tmp-" + statusId);
        String sourceScanId = getSourceScanId();
        while (true) {
          ScanEntity sourceScanEntity = scanPersistenceService.getScan(application.getId(), sourceScanId);
          try {
            scanPersistenceService.copyScanFile(sourceScanEntity, tempScanEntity);
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

        ScanTriggerType scanTriggerType = ScanTriggerType.UNKNOWN;
        // For older evaluations, we do not have the ClientScanType. ClientScanType was hardcoded to Sonatype here, so
        // leaving Sonatype here for backwards compatibility. For new evaluations, we will have this information.
        ClientScanType clientScanType = ClientScanType.SONATYPE;

        PolicyEvaluation policyEvaluation =
            policyEvaluationDAO.getLastByApplicationIdAndScanId(applicationId, sourceScanId);
        if (policyEvaluation != null) {
          scanTriggerType = policyEvaluation.getScanTriggerType();
          if (policyEvaluation.getClientScanType() != null) {
            clientScanType = policyEvaluation.getClientScanType();
          }
        }

        policyEvaluateService.evaluateWithPolling(statusId, application, clientScanType,
            new Stage(targetStageId), scanTriggerType, tempScanEntity, "api", userAgent, null);
      }
      catch (Exception e) {
        log.error("Failed to promote scan of app {} to stage {}. The status ID of the operation is {}.", applicationId,
            targetStageId, statusId);
        PolicyEvaluationPollingResult policyEvaluationPollingResult =
            policyEvaluationPollingResultUtils.handleException(applicationId, statusId, e);

        throw new RuntimeException(policyEvaluationPollingResult.getReason(), e);
      }
    }

    private String getSourceScanId() {
      if (apiPromoteScanRequestDTOV2.scanId != null) {
        return apiPromoteScanRequestDTOV2.scanId;
      }
      return getLastEvaluation(applicationId, apiPromoteScanRequestDTOV2.sourceStageId).getScanId();
    }
  }
}
