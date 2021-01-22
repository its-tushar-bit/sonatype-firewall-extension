/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluationTriggerType;
import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluatorResults;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiPromoteScanServiceV2Test
    extends AbstractComponentTest
{
  private static final String SCAN_ID = "scanId";

  private static final String NEW_SCAN_ID = "newScanId";

  private Application app;

  @Mock
  private ScanUploader scanUploader;

  @Mock
  private ReportDownloader reportDownloader;

  @Mock
  private PolicyAlertNotifier policyAlertNotifier;

  @Mock
  private ScanPolicyEvaluator scanPolicyEvaluator;

  @Inject
  private ApiPromoteScanServiceV2 service;

  @Inject
  private InsightWork insightWork;

  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(ScanUploader.class).toInstance(scanUploader);
    binder.bind(ReportDownloader.class).toInstance(reportDownloader);
    binder.bind(PolicyAlertNotifier.class).toInstance(policyAlertNotifier);
    binder.bind(ScanPolicyEvaluator.class).toInstance(scanPolicyEvaluator);
    super.configure(binder);
  }

  @Test
  public void testPromoteScan_FromScanId() {
    createScanFile();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);

    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 = service
        .promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE), null /* userAgent */);

    assertThat(apiApplicationEvaluationStatusDTOV2).isNotNull();
    assertThat(apiApplicationEvaluationStatusDTOV2.statusUrl)
        .startsWith(String.format("api/v2/evaluation/applications/%s/status/", app.getId()));
  }

  @Test
  public void testPromoteScan_FromStageId() throws Exception {
    createScanFile();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(NEW_SCAN_ID);
    String toStageId = Stage.ID_OPERATE;
    when(scanUploader.upload(any(File.class), any(Application.class), anyString())).thenReturn(scanReceipt);
    ScanPolicyEvaluatorResults evaluatorResults = new ScanPolicyEvaluatorResults();
    evaluatorResults.evaluation = tempEntity.newPolicyEvaluation(app.getId(), toStageId, NEW_SCAN_ID);
    when(scanPolicyEvaluator.evaluate(any(Application.class), eq(NEW_SCAN_ID), any(Stage.class),
        eq(PolicyEvaluationTriggerType.CLI))).thenReturn(evaluatorResults);

    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 = service.promoteScan(app.getId(),
        ApiPromoteScanRequestDTOV2.fromStage(Stage.ID_BUILD, toStageId), null /* userAgent */);

    assertThat(apiApplicationEvaluationStatusDTOV2).isNotNull();
    assertThat(apiApplicationEvaluationStatusDTOV2.statusUrl)
        .startsWith(String.format("api/v2/evaluation/applications/%s/status/", app.getId()));

    // await successful completion
    String scanPromotionStatusId = getStatusId(apiApplicationEvaluationStatusDTOV2.statusUrl);
    awaitPromoteScanResult(scanPromotionStatusId, 1, TimeUnit.MINUTES);
    assertThat(insightWork.getScanFile(app.getId(), NEW_SCAN_ID)).isFile();
    verify(policyAlertNotifier).sendNotifications(any(Application.class), eq(evaluatorResults));
  }

  @Test
  public void testPromoteScan_NullRequestDTO() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.promoteScan(app.getId(), null, null /* userAgent */);
    }).withMessage("Missing parameters.");
  }

  @Test
  public void testPromoteScan_NoSourceScan() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromStage(null, Stage.ID_OPERATE),
          null /* userAgent */);
    }).withMessageStartingWith("Either scanId or sourceStageId need to be supplied.");
  }

  @Test
  public void testPromoteScan_AmbiguousSourceScan() {
    ApiPromoteScanRequestDTOV2 requestDTO = ApiPromoteScanRequestDTOV2.fromStage(Stage.ID_BUILD, Stage.ID_OPERATE);
    requestDTO.scanId = SCAN_ID;
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.promoteScan(app.getId(), requestDTO, null /* userAgent */);
    }).withMessageStartingWith("Only one of scanId or sourceStageId can be supplied.");
  }

  @Test
  public void testPromoteScan_NoEvaluationsInSourceStage() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromStage(Stage.ID_BUILD, Stage.ID_OPERATE),
          null /* userAgent */);
    }).withMessageStartingWith("No scan available to promote from stage");
  }

  @Test
  public void testPromoteScan_ScanDoesNotExist_Failed() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE),
          null /* userAgent */);
    }).withMessageStartingWith("A scan with ID " + SCAN_ID + " does not exist on the server and may be obsolete. ");
  }

  @Test
  public void testPromoteScan_InvalidStage_Failed() {
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    final List<String> invalidStages = Arrays.asList("invalidStage", Stage.ID_DEVELOP, Stage.ID_PROXY);
    for (String invalidStage : invalidStages) {
      assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
        service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, invalidStage),
            null /* userAgent */);
      }).withMessage("Stage " + invalidStage + " is invalid.");
    }
  }

  @Test
  public void testGetApplicationEvaluationStatus_Pending() throws Exception {
    createScanFile();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    lenient().doAnswer((Answer<ScanReceipt>) invocationOnMock -> {
      countDownLatch.await(1, TimeUnit.MINUTES);
      return null;
    }).when(scanUploader).upload(any(File.class), any(Application.class), anyString());
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 = service
        .promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE), null /* userAgent */);
    String scanPromotionStatusId = getStatusId(apiApplicationEvaluationStatusDTOV2.statusUrl);

    ApiApplicationEvaluationResultDTOV2 apiApplicationEvaluationResultDTOV2 =
        service.getApplicationEvaluationStatus(app.getId(), scanPromotionStatusId);
    countDownLatch.countDown();

    assertThat(apiApplicationEvaluationResultDTOV2).isNotNull();
    assertThat(apiApplicationEvaluationResultDTOV2.status).isEqualTo(PolicyEvaluationStatus.PENDING.name());
    assertThat(apiApplicationEvaluationResultDTOV2.reason).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.reportHtmlUrl).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.embeddableReportHtmlUrl).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.reportPdfUrl).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.reportDataUrl).isNull();
  }

  @Test
  public void testGetApplicationEvaluationStatus_Failure() throws Exception {
    createScanFile();
    when(scanUploader.upload(any(File.class), any(Application.class), anyString()))
        .thenThrow(new RuntimeException("ruh-roh"));
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 = service
        .promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE), null /* userAgent */);
    String scanPromotionStatusId = getStatusId(apiApplicationEvaluationStatusDTOV2.statusUrl);
    awaitPromoteScanResult(scanPromotionStatusId, 1, TimeUnit.MINUTES);

    ApiApplicationEvaluationResultDTOV2 apiApplicationEvaluationResultDTOV2 =
        service.getApplicationEvaluationStatus(app.getId(), scanPromotionStatusId);

    assertThat(apiApplicationEvaluationResultDTOV2).isNotNull();
    assertThat(apiApplicationEvaluationResultDTOV2.status).isEqualTo(PolicyEvaluationStatus.FAILED.name());
    assertThat(apiApplicationEvaluationResultDTOV2.reason).startsWith("Internal Server Error");
    assertThat(apiApplicationEvaluationResultDTOV2.reportHtmlUrl).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.embeddableReportHtmlUrl).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.reportPdfUrl).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.reportDataUrl).isNull();
  }

  private void awaitPromoteScanResult(String statusId, long timeout, TimeUnit unit) {
    await().atMost(timeout, unit)
        .until(() -> !PolicyEvaluationStatus.PENDING.equals(new PersistedPolicyEvaluationPollingResultDAO()
            .getByApplicationIdAndStatusId(app.getId(), statusId).getPolicyEvaluationPollingResult().getStatus()));
  }

  @Test
  public void testGetApplicationEvaluationStatus_Success() throws Exception {
    createScanFile();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(NEW_SCAN_ID);
    when(scanUploader.upload(any(File.class), any(Application.class), anyString())).thenReturn(scanReceipt);
    String toStageId = Stage.ID_OPERATE;
    ScanPolicyEvaluatorResults evaluatorResults = new ScanPolicyEvaluatorResults();
    evaluatorResults.evaluation = tempEntity.newPolicyEvaluation(app.getId(), toStageId, NEW_SCAN_ID);
    when(scanPolicyEvaluator.evaluate(any(Application.class), eq(NEW_SCAN_ID), any(Stage.class),
        eq(PolicyEvaluationTriggerType.CLI))).thenReturn(evaluatorResults);
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 = service
        .promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, toStageId), null /* userAgent */);
    String scanPromotionStatusId = getStatusId(apiApplicationEvaluationStatusDTOV2.statusUrl);
    awaitPromoteScanResult(scanPromotionStatusId, 1, TimeUnit.MINUTES);

    ApiApplicationEvaluationResultDTOV2 apiApplicationEvaluationResultDTOV2 =
        service.getApplicationEvaluationStatus(app.getId(), getStatusId(scanPromotionStatusId));

    assertThat(apiApplicationEvaluationResultDTOV2).isNotNull();
    assertThat(apiApplicationEvaluationResultDTOV2.status).isEqualTo(PolicyEvaluationStatus.COMPLETED.name());
    assertThat(apiApplicationEvaluationResultDTOV2.reason).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.reportHtmlUrl)
        .isEqualTo(String.format("ui/links/application/%s/report/%s", app.getPublicId(), NEW_SCAN_ID));
    assertThat(apiApplicationEvaluationResultDTOV2.embeddableReportHtmlUrl)
        .isEqualTo(String.format("ui/links/application/%s/report/%s/embeddable", app.getPublicId(), NEW_SCAN_ID));
    assertThat(apiApplicationEvaluationResultDTOV2.reportPdfUrl)
        .isEqualTo(String.format("ui/links/application/%s/report/%s/pdf", app.getPublicId(), NEW_SCAN_ID));
    assertThat(apiApplicationEvaluationResultDTOV2.reportDataUrl)
        .isEqualTo(String.format("api/v2/applications/%s/reports/%s/raw", app.getPublicId(), NEW_SCAN_ID));
  }

  @Test
  public void testGetApplicationEvaluationStatus_MismatchedAppStatusIds_NotFound() throws Exception {
    // app scan promotion
    createScanFile();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    String appScanPromotionStatusId = getStatusId(service.promoteScan(app.getId(),
        ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE), null /* userAgent */).statusUrl);

    // otherApp scan promotion
    Application otherApp = tempEntity.newApplicationWithParent();
    createScanFile(otherApp.getId());
    tempEntity.newPolicyEvaluation(otherApp.getId(), Stage.ID_BUILD, SCAN_ID);
    String otherAppScanPromotionStatusId = getStatusId(service.promoteScan(otherApp.getId(),
        ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE), null /* userAgent */).statusUrl);

    assertNotFound(app, getStatusId(otherAppScanPromotionStatusId));
    assertNotFound(otherApp, getStatusId(appScanPromotionStatusId));
  }

  private void assertNotFound(Application app, String statusId) {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.getApplicationEvaluationStatus(app.getId(), statusId);
    }).withMessage("Policy evaluation status with id %s for public application id %s was not found.", statusId,
        app.getPublicId());
  }

  private String getStatusId(String statusUrl) {
    return statusUrl.substring(statusUrl.lastIndexOf("/") + 1);
  }

  private void createScanFile() {
    createScanFile(app.getId());
  }

  private void createScanFile(String appId) {
    File scanFile = insightWork.getScanFile(appId, SCAN_ID);
    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), Collections.singletonList("test"));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
