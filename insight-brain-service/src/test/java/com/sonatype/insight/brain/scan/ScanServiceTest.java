/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.scan.PersistedScanTicket;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.scan.ScanTask.State;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

public class ScanServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ScanService scanService;

  @Inject
  private PersistedScanTicketDAO persistedScanTicketDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private ScanUploader scanUploader;

  @Mock
  private ReportDownloader reportDownloader;

  private Application app;

  private ScanTicket scanTicket;

  private InputStream getBundle(String name) {
    return getClass().getResourceAsStream("/ScanServiceTest/" + name);
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(ScanUploader.class).toInstance(scanUploader);
    binder.bind(ReportDownloader.class).toInstance(reportDownloader);
    super.configure(binder);
  }

  @Before
  public void init() throws Exception {
    app = tempEntity.newApplication(tempEntity.newOrganization().getId());
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("scan-id");
    lenient().when(scanUploader.upload((File) any(), any(Application.class), anyString(), eq(null)))
        .thenReturn(receipt);
    lenient().when(reportDownloader.downloadReport(eq(receipt.getScanId()), (File) any(), anyInt(), anyInt())).then(
        new Answer<Boolean>()
        {
          @Override
          public Boolean answer(InvocationOnMock invocation) throws Throwable {
            File reportFile = (File) invocation.getArguments()[1];
            FileUtils.copyURLToFile(ReportHelper.zipReport("/ScanServiceTest/report", tempDir),
                reportFile);
            return true;
          }
        });
  }

  @After
  public void exit() {
    // wait for any submitted scan to finish processing or its activity can affect following tests
    while (scanTicket != null && scanTicket.currentStep < scanTicket.totalSteps) {
      Thread.yield();
      scanTicket = scanService.getTicket(scanTicket.applicationPublicId, scanTicket.ticketId);
    }
  }

  @Test
  public void testScanBinary() throws Exception {
    InputStream appBundle = getBundle("app01.zip");
    scanTicket =
        scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(Stage.ID_BUILD), false, null, null);
    assertThat(scanTicket).isNotNull();
    assertThat(scanTicket.ticketId).isNotNull();
  }

  @Test
  public void testSaveBinary_KeepsOriginalFileExtensionForArchiveDetectionPurposes() throws Exception {
    File file = scanService.saveBinary(getBundle("app01.zip"), "app.tar.gz");
    file.delete();
    assertThat(file.getName()).endsWith(".tar.gz");
  }

  @Test
  public void testGetTicket() throws IOException {
    InputStream appBundle = getBundle("app01.zip");
    scanTicket =
        scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(Stage.ID_BUILD), false, null, null);

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
        Stage.ID_BUILD), false, null, null);

    ScanTicket statusTicket = originalTicket;
    while (statusTicket.currentStep != statusTicket.totalSteps) {
      statusTicket = scanService.getTicket(app.getPublicId(), originalTicket.ticketId);
    }

    assertThat(statusTicket).isNotNull();
    assertThat(statusTicket.error).isNull();
  }

  @Test
  public void testFailEarlyOnInvalidStage() throws Exception {
    InputStream appBundle = getBundle("app01.zip");
    assertThatExceptionOfType(InvalidStageException.class).isThrownBy(() -> {
      scanService
          .scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage("invalid-stage-id"), false, null, null);
    }).withMessageContaining("invalid-stage-id");
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

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      scanService.scanBinary(app.getPublicId(), appBundle, "app01.zip", new Stage(Stage.ID_BUILD), false, null, null);
    }).withMessage("Your IQ Server license does not enable this feature.");
  }
}
