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
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiScanResultDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiPromoteScanServiceV2.ScanStatus;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
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
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
    lenient().when(reportDownloader.downloadReport(eq(NEW_SCAN_ID), any(File.class), anyInt(), anyInt())).then(
        invocation -> {
          File reportFile = (File) invocation.getArguments()[1];
          FileUtils.copyURLToFile(getClass().getResource("/ApiPromoteScanServiceV2Test/report.zip"), reportFile);
          return true;
        });
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(ScanUploader.class).toInstance(scanUploader);
    binder.bind(ReportDownloader.class).toInstance(reportDownloader);
    binder.bind(PolicyAlertNotifier.class).toInstance(policyAlertNotifier);
    binder.bind(ScanPolicyEvaluator.class).toInstance(scanPolicyEvaluator);
  }

  @Test
  public void testPromoteScan() {
    createScanFile();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);

    ApiPromoteScanResultDTOV2 apiPromoteScanResultDTOV2 = service
        .promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE));

    assertThat(apiPromoteScanResultDTOV2, is(notNullValue()));
    assertThat(apiPromoteScanResultDTOV2.statusUrl,
        startsWith(String.format("api/v2/evaluation/applications/%s/status/", app.getId())));
  }

  @Test
  public void testPromoteScan_ScanDoesNotExist_Failed() {
    try {
      service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE));
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          startsWith("A scan with ID " + SCAN_ID + " does not exist on the server and may be obsolete. "));
    }
  }

  @Test
  public void testPromoteScan_InvalidStage_Failed() {
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    final List<String> invalidStages = Arrays.asList("invalidStage", Stage.ID_DEVELOP, Stage.ID_PROXY);
    for (String invalidStage : invalidStages) {
      try {
        service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, invalidStage));
        fail("Expected exception for stage " + invalidStage + ".");
      }
      catch (BadRequestException e) {
        assertThat(e.getMessage(), is("Stage " + invalidStage + " is invalid."));
      }
    }
  }

  @Test
  public void testGetScanStatus_Pending() throws Exception {
    createScanFile();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    lenient().doAnswer((Answer<ScanReceipt>) invocationOnMock -> {
      countDownLatch.await(1, TimeUnit.MINUTES);
      return null;
    }).when(scanUploader).upload(any(File.class), any(Application.class));
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    ApiPromoteScanResultDTOV2 apiPromoteScanResultDTOV2 = service
        .promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE));
    String scanPromotionKey = getScanPromotionKey(apiPromoteScanResultDTOV2.statusUrl);

    ApiScanResultDTOV2 scanStatus = service.getScanStatus(app.getId(), getStatusId(scanPromotionKey));
    countDownLatch.countDown();

    assertThat(scanStatus, is(notNullValue()));
    assertThat(scanStatus.status, is(ScanStatus.PENDING.name()));
    assertThat(scanStatus.reason, is(nullValue()));
    assertThat(scanStatus.reportHtmlUrl, is(nullValue()));
    assertThat(scanStatus.embeddableReportHtmlUrl, is(nullValue()));
    assertThat(scanStatus.reportPdfUrl, is(nullValue()));
    assertThat(scanStatus.reportDataUrl, is(nullValue()));
  }

  @Test
  public void testGetScanStatus_Failure() throws Exception {
    createScanFile();
    when(scanUploader.upload(any(File.class), any(Application.class))).thenThrow(new RuntimeException("ruh-roh"));
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    ApiPromoteScanResultDTOV2 apiPromoteScanResultDTOV2 = service
        .promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE));
    String scanPromotionKey = getScanPromotionKey(apiPromoteScanResultDTOV2.statusUrl);
    try {
      service.scanPromotions.getIfPresent(scanPromotionKey).get(1, TimeUnit.MINUTES);
    }
    catch (Exception e) {
      // do nothing
    }

    ApiScanResultDTOV2 scanStatus = service.getScanStatus(app.getId(), getStatusId(scanPromotionKey));

    assertThat(scanStatus, is(notNullValue()));
    assertThat(scanStatus.status, is(ScanStatus.FAILED.name()));
    assertThat(scanStatus.reason, startsWith("Internal Server Error"));
    assertThat(scanStatus.reportHtmlUrl, is(nullValue()));
    assertThat(scanStatus.embeddableReportHtmlUrl, is(nullValue()));
    assertThat(scanStatus.reportPdfUrl, is(nullValue()));
    assertThat(scanStatus.reportDataUrl, is(nullValue()));
  }

  @Test
  public void testGetScanStatus_Success() throws Exception {
    createScanFile();
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(NEW_SCAN_ID);
    when(scanUploader.upload(any(File.class), any(Application.class))).thenReturn(scanReceipt);
    when(scanPolicyEvaluator.evaluate(any(Application.class), eq(NEW_SCAN_ID), any(Stage.class)))
        .thenReturn(new ScanPolicyEvaluatorResults());
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    ApiPromoteScanResultDTOV2 apiPromoteScanResultDTOV2 = service
        .promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE));
    String scanPromotionKey = getScanPromotionKey(apiPromoteScanResultDTOV2.statusUrl);
    service.scanPromotions.getIfPresent(scanPromotionKey).get(1, TimeUnit.MINUTES);

    ApiScanResultDTOV2 scanStatus = service.getScanStatus(app.getId(), getStatusId(scanPromotionKey));

    assertThat(scanStatus, is(notNullValue()));
    assertThat(scanStatus.status, is(ScanStatus.COMPLETED.name()));
    assertThat(scanStatus.reason, is(nullValue()));
    assertThat(scanStatus.reportHtmlUrl,
        is(String.format("ui/links/application/%s/report/%s", app.getPublicId(), NEW_SCAN_ID)));
    assertThat(scanStatus.embeddableReportHtmlUrl,
        is(String.format("ui/links/application/%s/report/%s/embeddable", app.getPublicId(), NEW_SCAN_ID)));
    assertThat(scanStatus.reportPdfUrl,
        is(String.format("ui/links/application/%s/report/%s/pdf", app.getPublicId(), NEW_SCAN_ID)));
    assertThat(scanStatus.reportDataUrl,
        is(String.format("api/v2/applications/%s/reports/%s", app.getPublicId(), NEW_SCAN_ID)));
  }

  @Test
  public void testGetScanStatus_MismatchedAppStatusIds_NotFound() throws Exception {
    // app scan promotion
    createScanFile();
    tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, SCAN_ID);
    String appScanPromotionKey = getScanPromotionKey(
        service.promoteScan(app.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE)).statusUrl);

    // otherApp scan promotion
    Application otherApp = tempEntity.newApplicationWithParent();
    createScanFile(otherApp.getId());
    tempEntity.newPolicyEvaluation(otherApp.getId(), Stage.ID_BUILD, SCAN_ID);
    String otherAppScanPromotionKey = getScanPromotionKey(
        service
            .promoteScan(otherApp.getId(), ApiPromoteScanRequestDTOV2.fromScan(SCAN_ID, Stage.ID_OPERATE)).statusUrl);

    assertNotFound(app.getId(), getStatusId(otherAppScanPromotionKey));
    assertNotFound(otherApp.getId(), getStatusId(appScanPromotionKey));
  }

  private void assertNotFound(String applicationId, String statusId) {
    try {
      service.getScanStatus(applicationId, statusId);
      fail("Expected exception.");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is(String
          .format("Scan status with id %s for application with id %s was not found.", statusId, applicationId)));
    }
  }

  private String getScanPromotionKey(String statusUrl) {
    return app.getId() + ":" + statusUrl.substring(statusUrl.lastIndexOf("/") + 1);
  }

  private String getStatusId(String scanPromotionKey) {
    return scanPromotionKey.substring(scanPromotionKey.indexOf(":") + 1);
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
