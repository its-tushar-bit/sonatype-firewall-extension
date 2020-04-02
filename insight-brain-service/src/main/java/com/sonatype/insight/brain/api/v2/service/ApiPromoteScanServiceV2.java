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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiScanResultDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.51.0
 */
@Named
@Singleton
public class ApiPromoteScanServiceV2
{
  private static final Logger log = LoggerFactory.getLogger(ApiPromoteScanServiceV2.class);

  private final ThreadPoolExecutor executor;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ScanPolicyEvaluator scanPolicyEvaluator;

  private final PolicyAlertNotifier policyAlertNotifier;

  private final InsightWork work;

  private final ScanUploader uploader;
  
  private final ErrorResponseGenerator errorResponseGenerator;

  @VisibleForTesting
  final Cache<String, Future<String>> scanPromotions = CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.HOURS)
      .build();

  enum ScanStatus
  {
    PENDING,
    COMPLETED,
    FAILED
  }

  @Inject
  public ApiPromoteScanServiceV2(ApplicationDAO applicationDAO,
                                 PolicyEvaluationDAO policyEvaluationDAO,
                                 ScanPolicyEvaluator scanPolicyEvaluator,
                                 PolicyAlertNotifier policyAlertNotifier,
                                 InsightWork work,
                                 ScanUploader uploader,
                                 ErrorResponseGenerator errorResponseGenerator)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.work = work;
    this.uploader = uploader;
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
  public ApiPromoteScanResultDTOV2 promoteScan(
      @AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
      final ApiPromoteScanRequestDTOV2 apiPromoteScanRequestDTOV2)
  {
    final Application application = applicationDAO.getByIdNotNull(applicationId);

    validateRequest(apiPromoteScanRequestDTOV2, application.getId());
    String statusId = UUID.randomUUID().toString().replace("-", "");
    log.debug("Received request to promote scan {} of app {} to stage {}. The status ID of the operation is {}.",
        apiPromoteScanRequestDTOV2.scanId != null ? apiPromoteScanRequestDTOV2.scanId
            : "from stage " + apiPromoteScanRequestDTOV2.sourceStageId,
        application.getName(), apiPromoteScanRequestDTOV2.targetStageId, statusId);

    scanPromotions.put(getScanPromotionKey(application.getId(), statusId), AuditData.get()
        .continueAsync(new ScanPromotionTask(apiPromoteScanRequestDTOV2, application.getId(), statusId),
            executor::submit));

    ApiPromoteScanResultDTOV2 apiPromoteScanResultDTOV2 = new ApiPromoteScanResultDTOV2();
    apiPromoteScanResultDTOV2.statusUrl = getStatusUrl(applicationId, statusId);
    return apiPromoteScanResultDTOV2;
  }

  private String getScanPromotionKey(String applicationId, String statusId) {
    return applicationId + ":" + statusId;
  }

  class ScanPromotionTask
      implements Callable<String>
  {
    private final ApiPromoteScanRequestDTOV2 apiPromoteScanRequestDTOV2;

    private final String applicationId;

    private final String statusId;

    ScanPromotionTask(final ApiPromoteScanRequestDTOV2 apiPromoteScanRequestDTOV2,
                      final String applicationId,
                      final String statusId)
    {
      this.apiPromoteScanRequestDTOV2 = apiPromoteScanRequestDTOV2;
      this.applicationId = applicationId;
      this.statusId = statusId;
    }

    @Override
    public String call() throws Exception {
      File tempScanFile = null;
      final String targetStageId = apiPromoteScanRequestDTOV2.targetStageId;
      try {
        final long start = System.currentTimeMillis();

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

        final ScanReceipt scanReceipt = uploader.upload(tempScanFile, application, targetStageId);
        scanReceipt.waitForReport();
        Files.move(tempScanFile.toPath(), work.getScanFile(application.getId(), scanReceipt.getScanId()).toPath());
        ScanPolicyEvaluatorResults results = scanPolicyEvaluator
            .evaluate(application, scanReceipt.getScanId(), new Stage(targetStageId));
        policyAlertNotifier.sendNotifications(application, results);
        log.debug("Promoted scan {} of app {} to stage {} in {} ms. The status ID of the operation is {}.",
            sourceScanId, application.getName(), targetStageId, System.currentTimeMillis() - start, statusId);
        return scanReceipt.getScanId();
      }
      catch (Exception e) {
        log.error("Failed to promote scan of app {} to stage {}. The status ID of the operation is {}.", applicationId,
            targetStageId, statusId);
        throw new RuntimeException(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody(), e);
      }
      finally {
        if (tempScanFile != null && tempScanFile.exists() && !tempScanFile.delete()) {
          log.warn("Failed to delete temporary scan file {}.", tempScanFile);
        }
      }
    }

    private String getSourceScanId() {
      if (apiPromoteScanRequestDTOV2.scanId != null) {
        return apiPromoteScanRequestDTOV2.scanId;
      }
      return getLastEvaluation(applicationId, apiPromoteScanRequestDTOV2.sourceStageId).getScanId();
    }
  }

  private static String getStatusUrl(String applicationId, String statusId) {
    return UriBuilder.fromPath(PublicApiPaths.PROMOTE_SCAN_STATUS_PATH_V2).build(applicationId, statusId).toString();
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION)
  public ApiScanResultDTOV2 getScanStatus(@AuthzContext(AuthzContext.Key.APPLICATION_ID) final String applicationId,
                                          String statusId)
  {
    Future<String> scan = scanPromotions.getIfPresent(getScanPromotionKey(applicationId, statusId));
    if (scan == null) {
      throw new NotFoundException(
          String.format("Scan status with id %s for application with id %s was not found.", statusId, applicationId));
    }
    ApiScanResultDTOV2 scanStatus = new ApiScanResultDTOV2();
    if (!scan.isDone()) {
      scanStatus.status = ScanStatus.PENDING.name();
      return scanStatus;
    }
    String scanId;
    try {
      scanId = scan.get();
    }
    catch (Exception e) {
      scanStatus.status = ScanStatus.FAILED.name();
      scanStatus.reason = e.getCause().getMessage();
      return scanStatus;
    }
    scanStatus.status = ScanStatus.COMPLETED.name();
    String applicationPublicId = applicationDAO.getByIdNotNull(applicationId).getPublicId();
    scanStatus.reportPdfUrl = UserInterfaceLinksResource.getPdfUrl(applicationPublicId, scanId);
    scanStatus.reportHtmlUrl = UserInterfaceLinksResource.getReportUrl(applicationPublicId, scanId);
    scanStatus.embeddableReportHtmlUrl = UserInterfaceLinksResource.getEmbeddableReportUrl(applicationPublicId, scanId);
    scanStatus.reportDataUrl = ApiReportDataResourceV2.getDataUrl(applicationPublicId, scanId);
    return scanStatus;
  }
}
