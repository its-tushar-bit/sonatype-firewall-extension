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
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
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
                                 ScanPolicyEvaluator scanPolicyEvaluator,
                                 PolicyAlertNotifier policyAlertNotifier,
                                 InsightWork work,
                                 ScanUploader uploader,
                                 ErrorResponseGenerator errorResponseGenerator)
  {
    this.applicationDAO = applicationDAO;
    this.scanPolicyEvaluator = scanPolicyEvaluator;
    this.policyAlertNotifier = policyAlertNotifier;
    this.work = work;
    this.uploader = uploader;
    this.errorResponseGenerator = errorResponseGenerator;

    executor = new ThreadPoolExecutor(100, 100, 5L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat("ApiPromoteScanServiceV2-%d").build());
    executor.allowCoreThreadTimeOut(true);
  }

  private void validateRequest(final String scanId, final String stageId, final String applicationId) {
    if (!isValidTargetStage(stageId)) {
      throw new BadRequestException("Stage " + stageId + " is invalid.");
    }

    final File scanFile = work.getScanFile(applicationId, scanId);
    if (!scanFile.isFile()) {
      throw new BadRequestException("A scan with ID " + scanId +
          " does not exist on the server and may be obsolete. Note that only the most recent scan for the given" +
          " stage can be promoted.");
    }
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

    validateRequest(apiPromoteScanRequestDTOV2.scanId, apiPromoteScanRequestDTOV2.targetStageId, application.getId());
    String statusId = UUID.randomUUID().toString().replace("-", "");
    log.debug("Received request to promote scan {} of app {} to stage {}. The status ID of the operation is {}.",
        apiPromoteScanRequestDTOV2.scanId, application.getName(), apiPromoteScanRequestDTOV2.targetStageId, statusId);

    scanPromotions.put(getScanPromotionKey(application.getId(), statusId),
        executor.submit(new ScanPromotionTask(apiPromoteScanRequestDTOV2, applicationId, statusId)));

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
      final String scanId = apiPromoteScanRequestDTOV2.scanId;
      try {
        final long start = System.currentTimeMillis();

        final Application application = applicationDAO.getByIdNotNull(applicationId);

        log.debug("Promoting scan {} of app {} to stage {}. The status ID of the operation is {}.", scanId,
            application.getName(), targetStageId, statusId);
        tempScanFile = work.getScanFile(applicationId, "tmp-" + scanId);

        final ScanReceipt scanReceipt = uploadNewScanFile(application, scanId, tempScanFile);
        scanReceipt.waitForReport();
        Files.move(tempScanFile.toPath(), work.getScanFile(application.getId(), scanReceipt.getScanId()).toPath());
        ScanPolicyEvaluatorResults results = scanPolicyEvaluator
            .evaluate(application, scanReceipt.getScanId(), new Stage(targetStageId));
        policyAlertNotifier.sendNotifications(application, results);
        log.debug("Promoted scan {} of app {} to stage {} in {} ms. The status ID of the operation is {}.", scanId,
            application.getName(), targetStageId, System.currentTimeMillis() - start, statusId);
        return scanReceipt.getScanId();
      }
      catch (Exception e) {
        log.error("Failed to promote scan {} of app {} to stage {}. The status ID of the operation is {}.",
            scanId, applicationId, targetStageId, statusId);
        throw new RuntimeException(errorResponseGenerator.mapExceptionAndLog(e).getMessageBody(), e);
      }
      finally {
        if (tempScanFile != null && tempScanFile.exists() && !tempScanFile.delete()) {
          log.warn("Failed to delete temporary scan file {}.", tempScanFile);
        }
      }
    }
  }

  private ScanReceipt uploadNewScanFile(Application application, String scanId, File tempScanFile) throws IOException {
    final File sourceScanFile = work.getScanFile(application.getId(), scanId);
    Files.copy(sourceScanFile.toPath(), tempScanFile.toPath());
    return uploader.upload(tempScanFile, application);
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
