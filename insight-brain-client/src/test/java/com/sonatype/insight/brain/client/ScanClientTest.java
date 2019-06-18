/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.http.client.HttpResponseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ScanClientTest
    extends AbstractBrainServiceTest
{
  private static final String APP_ID = "ScanClientTest_AppId";

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Before
  public void createApplication() {
    tempEntity.newApplicationWithParent(APP_ID, "test");
  }

  @Test
  public void testUploadRepoManScan() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ScanReceipt receipt = new ScanClient(config, APP_ID).uploadRepoManScan(tmpDir.newFile("scan.xml.gz"));
    assertThat(receipt.getScanId()).isEqualTo("SCAN-ID");
    assertThat(receipt.getReportUrl()).isEqualTo("ui/links/application/ScanClientTest_AppId/report/SCAN-ID");
    assertThat(receipt.getPdfUrl()).isEqualTo("ui/links/application/ScanClientTest_AppId/report/SCAN-ID/pdf");
  }

  @Test
  public void testUploadRepoManScan_InvalidAppId() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    try {
      new ScanClient(config, "invalid-id").uploadRepoManScan(tmpDir.newFile("scan.xml.gz"));
      fail("Upload should have failed due to invalid app ID");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode()).isEqualTo(404);
      assertThat(e.getMessage()).isEqualTo("Could not find an application with public ID invalid-id.");
    }
  }

  @Test
  public void testSaveResultData() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("the-scan-id");
    receipt.setReportUrl("the-report-url");
    receipt.setPdfUrl("the-pdf-url");
    receipt.setDataUrl("the-data-url");
    File resultFile = new File(tmpDir.getRoot(), "missing-dir/result.json");
    new ScanClient(config, "the-app-id").saveResultData(resultFile, receipt);
    ResultData data = JsonUtils.read(resultFile, ResultData.class);
    assertThat(data.scanId).isEqualTo(receipt.getScanId());
    assertThat(data.applicationId).isEqualTo("the-app-id");
    assertThat(data.reportHtmlUrl).isEqualTo(receipt.resolveReportUrl(config.getServerUrl()));
    assertThat(data.reportPdfUrl).isEqualTo(receipt.resolvePdfUrl(config.getServerUrl()));
    assertThat(data.reportDataUrl).isEqualTo(receipt.resolveDataUrl(config.getServerUrl()));
  }

  @Test
  public void testUploadCLIScan() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ScanReceipt receipt = new ScanClient(config, APP_ID).uploadCLIScan(tmpDir.newFile("scan.xml.gz"),
        ClientScanType.SONATYPE);
    assertThat(receipt.getScanId()).isEqualTo("SCAN-ID");
    assertThat(receipt.getReportUrl()).isEqualTo("ui/links/application/ScanClientTest_AppId/report/SCAN-ID");
    assertThat(receipt.getPdfUrl()).isEqualTo("ui/links/application/ScanClientTest_AppId/report/SCAN-ID/pdf");
  }

  @Test
  public void testUploadCLIScan_InvalidAppId() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    try {
      new ScanClient(config, "invalid-id").uploadCLIScan(tmpDir.newFile("scan.xml.gz"), ClientScanType.SONATYPE);
      fail("Upload should have failed due to invalid app ID");
    }
    catch (HttpResponseException e) {
      assertThat(e.getStatusCode()).isEqualTo(404);
      assertThat(e.getMessage()).isEqualTo("Could not find an application with public ID invalid-id.");
    }
  }
}
