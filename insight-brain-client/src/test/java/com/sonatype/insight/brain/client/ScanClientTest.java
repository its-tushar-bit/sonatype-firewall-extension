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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
  public void testUploadCIScan() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ScanReceipt receipt = new ScanClient(config, APP_ID).uploadCIScan(tmpDir.newFile("scan.xml.gz"));
    assertEquals("SCAN-ID", receipt.getScanId());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID", receipt.getReportUrl());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID/pdf", receipt.getPdfUrl());
  }

  @Test
  public void testUploadCIScan_InvalidAppId() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    try {
      new ScanClient(config, "invalid-id").uploadCIScan(tmpDir.newFile("scan.xml.gz"));
      fail("Upload should have failed due to invalid app ID");
    }
    catch (HttpResponseException e) {
      assertEquals(404, e.getStatusCode());
      assertEquals("Could not find an application with public ID invalid-id.", e.getMessage());
    }
  }

  @Test
  public void testUploadRepoManScan() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ScanReceipt receipt = new ScanClient(config, APP_ID).uploadRepoManScan(tmpDir.newFile("scan.xml.gz"));
    assertEquals("SCAN-ID", receipt.getScanId());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID", receipt.getReportUrl());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID/pdf", receipt.getPdfUrl());
  }

  @Test
  public void testUploadRepoManScan_InvalidAppId() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    try {
      new ScanClient(config, "invalid-id").uploadRepoManScan(tmpDir.newFile("scan.xml.gz"));
      fail("Upload should have failed due to invalid app ID");
    }
    catch (HttpResponseException e) {
      assertEquals(404, e.getStatusCode());
      assertEquals("Could not find an application with public ID invalid-id.", e.getMessage());
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
    assertThat(data.scanId, is(receipt.getScanId()));
    assertThat(data.applicationId, is("the-app-id"));
    assertThat(data.reportHtmlUrl, is(receipt.resolveReportUrl(config.getServerUrl())));
    assertThat(data.reportPdfUrl, is(receipt.resolvePdfUrl(config.getServerUrl())));
    assertThat(data.reportDataUrl, is(receipt.resolveDataUrl(config.getServerUrl())));
  }

  @Test
  public void testUploadCLIScan() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    ScanReceipt receipt = new ScanClient(config, APP_ID).uploadCLIScan(tmpDir.newFile("scan.xml.gz"),
        ClientScanType.SONATYPE);
    assertEquals("SCAN-ID", receipt.getScanId());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID", receipt.getReportUrl());
    assertEquals("ui/links/application/ScanClientTest_AppId/report/SCAN-ID/pdf", receipt.getPdfUrl());
  }

  @Test
  public void testUploadCLIScan_InvalidAppId() throws Exception {
    Configuration config = getCLMServer().getClientConfiguration();
    try {
      new ScanClient(config, "invalid-id").uploadCLIScan(tmpDir.newFile("scan.xml.gz"), ClientScanType.SONATYPE);
      fail("Upload should have failed due to invalid app ID");
    }
    catch (HttpResponseException e) {
      assertEquals(404, e.getStatusCode());
      assertEquals("Could not find an application with public ID invalid-id.", e.getMessage());
    }
  }
}
