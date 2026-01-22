/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.scan.PersistedScanTicket;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.scan.ScanTask.State;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.HdsMockServerRule;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScanServiceTest
    extends AbstractComponentTest
{
  @ClassRule
  public static HdsMockServerRule hdsMockServer = new HdsMockServerRule();

  @Inject
  private ScanService scanService;

  @Inject
  private PersistedScanTicketDAO persistedScanTicketDAO;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private ScanUploadService scanUploader;

  @Mock
  private ReportDownloader reportDownloader;

  @Inject
  private Configuration configuration;

  @Inject
  private InsightWork insightWork;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private Application app;

  private ScanTicket scanTicket;

  private InputStream getBundle(String name) {
    return getClass().getResourceAsStream("/ScanServiceTest/" + name);
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(ScanUploadService.class).toInstance(scanUploader);
    binder.bind(ReportDownloader.class).toInstance(reportDownloader);
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
    super.configure(binder);
  }

  @Before
  public void init() throws Exception {
    app = tempEntity.newApplication(tempEntity.newOrganization().getId());
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-id");
    lenient().when(scanUploader.upload(any(), any(Application.class), anyString(), any(), eq(null), any(), any(),
        anyBoolean())).thenReturn(receipt);
    lenient().when(reportDownloader.downloadReport(any(ApplicationReport.class), anyInt(), anyInt())).then(
        (Answer<Boolean>) invocation -> {
          ApplicationReport reportFile = (ApplicationReport) invocation.getArguments()[0];
          Application app = reportFile.getApplication();
          ReportHelper.saveMockReport(insightWork, tempDir, "/ScanServiceTest/report", app.getId(),
              reportFile.getScanId());
          return true;
        });
    hdsMockServer.reset();
    setHdsUrl(hdsMockServer.getHttpUrl());
  }

  @After
  public void exit() {
    // wait for any submitted scan to finish processing or its activity can affect following tests
    waitForScanResults();
  }

  private void waitForScanResults() {
    while (scanTicket != null && scanTicket.currentStep < scanTicket.totalSteps) {
      Thread.yield();
      scanTicket = scanService.getTicket(scanTicket.applicationPublicId, scanTicket.ticketId);
    }
  }

  @Test
  public void testScanService_AddsExecutorToShutdownHandler() {
    TenantThreadPoolExecutor tenantThreadPoolExecutor = scanService.getExecutors().get();

    verify(mockShutdownHandler).add(tenantThreadPoolExecutor);
  }

  @Test
  public void testScanBinary() throws Exception {
    InputStream appBundle = getBundle("app01.zip");
    scanTicket = scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(Stage.ID_BUILD), false,
        null, null, null);
    assertThat(scanTicket).isNotNull();
    assertThat(scanTicket.ticketId).isNotNull();
    
    waitForScanResults();
    PolicyEvaluation policyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanTicket.scanId);
    assertThat(policyEvaluation.getClientScanType()).isEqualTo(ClientScanType.SONATYPE);
    assertThat(policyEvaluation.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(policyEvaluation.getScanTriggerType()).isEqualTo(ScanTriggerType.WEB_UI);
  }

  @Test
  public void testScanBinary_WithThirdPartyContent() throws Exception {
    InputStream appBundle = getBundle("app1-bom.xml");
    scanTicket =
        scanService.scanBinary(app.getPublicId(), appBundle, "app1-bom.xml", new Stage(Stage.ID_BUILD), false, null,
            null, null);
    assertThat(scanTicket).isNotNull();
    assertThat(scanTicket.ticketId).isNotNull();

    waitForScanResults();
    PolicyEvaluation policyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanTicket.scanId);
    assertThat(policyEvaluation.getClientScanType()).isEqualTo(ClientScanType.SONATYPE_THIRD_PARTY);
    assertThat(policyEvaluation.getStageTypeId()).isEqualTo(Stage.ID_BUILD);
    assertThat(policyEvaluation.getScanTriggerType()).isEqualTo(ScanTriggerType.WEB_UI);
  }

  @Test
  public void testScanBinary_QueuePerTenant() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    List<String> t1TicketIds = new ArrayList<>();

    // Setup mock scan uploader so that it blocks on tenant 1 scans
    Mockito.reset(scanUploader);
    when(scanUploader.upload(any(), any(Application.class), anyString(), any(ClientScanType.class), eq(null),
        any(), any(), anyBoolean()))
        .thenAnswer(invocation -> {
          String appPublicId = ((Application) invocation.getArgument(1)).getPublicId();
          if (appPublicId.startsWith("t1") &&
              !countDownLatch.await(5, TimeUnit.SECONDS)) {
            return null;
          }
          ScanReceipt receipt = new ScanReceipt();
          receipt.setScanId("scan-id-" + appPublicId);
          return receipt;
        });
    Mockito.reset(reportDownloader);
    when(reportDownloader.downloadReport(any(ApplicationReport.class), anyInt(), anyInt()))
        .then((Answer<Boolean>) invocation -> {
          ApplicationReport reportFile = (ApplicationReport) invocation.getArguments()[1];
          Application app = reportFile.getApplication();
          ReportHelper.saveMockReport(insightWork, tempDir, "/ScanServiceTest/report", app.getId(),
              reportFile.getScanId());
          return true;
        });

    try {
      TenantTestHelper.initMultiTenantMode();

      // Start 2 binary scans for tenant 1 which should get blocked
      Tenant tenant1 = TenantTestHelper.testAsNewTenant(testName, t1 -> {
        configuration.register();
        Application t1App1 = tempEntity.newApplicationWithParent("t1-app1");
        Application t1App2 = tempEntity.newApplicationWithParent("t1-app2");

        ScanTicket scanTicket1 =
            scanService.scanBinary(t1App1.getPublicId(), getBundle("app01.zip"), "app01.zip", new Stage(Stage.ID_BUILD),
                false, null, null, null);
        assertThat(scanTicket1).isNotNull();
        assertThat(scanTicket1.ticketId).isNotNull();
        t1TicketIds.add(scanTicket1.ticketId);

        ScanTicket scanTicket2 =
            scanService.scanBinary(t1App2.getPublicId(), getBundle("app01.zip"), "app01.zip", new Stage(Stage.ID_BUILD),
                false, null, null, null);
        assertThat(scanTicket2).isNotNull();
        assertThat(scanTicket2.ticketId).isNotNull();
        t1TicketIds.add(scanTicket2.ticketId);
      });

      // Start 1 binary scan for tenant 2 and check that it completes (i.e. is not blocked by scans from tenant 1)
      TenantTestHelper.testAsNewTenant(testName, t2 -> {
        configuration.register();
        Application t2App1 = tempEntity.newApplicationWithParent("t2-app1");

        ScanTicket scanTicket3 =
            scanService.scanBinary(t2App1.getPublicId(), getBundle("app01.zip"), "app01.zip", new Stage(Stage.ID_BUILD),
                false, null, null, null);
        assertThat(scanTicket3).isNotNull();
        assertThat(scanTicket3.ticketId).isNotNull();

        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> persistedScanTicketDAO.getById(scanTicket3.ticketId).getStateId().equals(State.DONE.name()));
      });

      // Check scans from tenant 1 are still blocked, unblock them, and check that they complete
      TenantTestHelper.testAsTenantAndInvalidate(tenant1.tenantSlug, t -> {
        for (String t1TicketId : t1TicketIds) {
          assertThat(persistedScanTicketDAO.getById(t1TicketId)).isNotNull().extracting(PersistedScanTicket::getStateId)
              .isEqualTo(State.UPLOADING_SCAN.name());
        }

        countDownLatch.countDown();

        await().atMost(5, TimeUnit.SECONDS).until(
            () -> persistedScanTicketDAO.getAll().stream().map(PersistedScanTicket::getStateId)
                .allMatch(s -> s.equals(State.DONE.name())));
      });
    }
    finally {
      TenantTestHelper.resetAfterTest();
    }
  }

  @Test
  public void testSaveBinary_KeepsOriginalFileNameForDetectionPurposes() throws Exception {
    File file = scanService.saveBinary(getBundle("app01.zip"), "app.tar.gz");
    file.delete();
    assertThat(file.getName()).isEqualTo("app.tar.gz");
  }

  @Test
  public void testSaveBinary_DoesNotUseDirectoryInformation() throws Exception {
    String[] paths = new String[]{"dir1/app.tar.gz", "../app.tar.gz", "/dir2/../app.tar.gz", "/dir1/dir2/app.tar.gz"};

    for (String path : paths) {
      File file = scanService.saveBinary(getBundle("app01.zip"), path);
      file.delete();
      assertThat(file.getName()).isEqualTo("app.tar.gz");
    }
  }

  @Test
  public void testScanBinary_ThrowsException_WhenDirectory_WithForwardSlash() throws Exception {
    String filePath = "dir1/app01.zip";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      InputStream appBundle = getBundle("app01.zip");
      scanTicket = scanService.scanBinary(app.getPublicId(), appBundle, filePath, new Stage(Stage.ID_BUILD), false,
              null, null, null);
    }).withMessage("Filename must not be a directory: " + filePath);
  }

  @Test
  public void testScanBinary_ThrowsException_WhenDirectory_WithBackSlash() throws Exception {
    String filePath = "dir1\\app01.zip";

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      InputStream appBundle = getBundle("app01.zip");
      scanTicket = scanService.scanBinary(app.getPublicId(), appBundle, filePath, new Stage(Stage.ID_BUILD), false,
              null, null, null);
    }).withMessage("Filename must not be a directory: " + filePath);
  }

  @Test
  public void testGetTicket() throws IOException {
    InputStream appBundle = getBundle("app01.zip");
    scanTicket = scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(Stage.ID_BUILD), false,
        null, null, null);

    ScanTicket statusTicket = scanService.getTicket(app.getPublicId(), scanTicket.ticketId);
    assertThat(statusTicket.ticketId).isEqualTo(scanTicket.ticketId);
  }

  /**
   * Simulates what the UI will do, but without any pausing. Don't really know the value of this other than having an
   * integrated test of the actual task execution.
   */
  @Test(timeout = 15 * 1000)
  public void testGetTicketUntilTaskComplete() throws IOException {
    InputStream appBundle = getBundle("app01.zip");
    ScanTicket originalTicket = scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(
        Stage.ID_BUILD), false, null, null, null);

    ScanTicket statusTicket = originalTicket;
    while (statusTicket.currentStep != statusTicket.totalSteps) {
      statusTicket = scanService.getTicket(app.getPublicId(), originalTicket.ticketId);
    }

    assertThat(statusTicket).isNotNull();
    assertThat(statusTicket.error).isNull();
  }

  @Test
  public void testFailEarlyOnInvalidStage() {
    InputStream appBundle = getBundle("app01.zip");
    assertThatExceptionOfType(InvalidStageException.class).isThrownBy(() -> scanService
        .scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage("invalid-stage-id"), false, null, null, null))
        .withMessageContaining("invalid-stage-id");
  }

  @Test
  public void testGetTicket_NotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> scanService.getTicket("appPublicId", "doesNotExist"))
        .withMessageContaining("Cannot find ScanTicket with ID doesNotExist.");
  }

  @Test
  public void testGetTicket_DeletesIfDone() {
    PersistedScanTicket persistedScanTicket = new PersistedScanTicket();
    persistedScanTicket.setApplicationId(app.getId());
    persistedScanTicket.setStateId(State.DONE.name());
    persistedScanTicketDAO.insert(persistedScanTicket);

    scanService.getTicket(app.getPublicId(), persistedScanTicket.getId());

    assertThat(persistedScanTicketDAO.getById(persistedScanTicket.getId())).isNull();
  }

  @Test
  public void testScanBinary_FailsWithoutFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.CLI_INTEGRATION);
    InputStream appBundle = getBundle("app01.zip");

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> scanService
        .scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(Stage.ID_BUILD), false, null, null, null))
        .withMessage("Your IQ Server license does not enable this feature.");
  }
}
