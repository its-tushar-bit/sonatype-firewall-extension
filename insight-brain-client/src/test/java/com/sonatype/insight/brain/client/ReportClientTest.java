/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.client;

import java.io.File;
import java.util.zip.ZipFile;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ReportClientTest
    extends AbstractBrainServiceTest
{
  private static final String applicationPublicId = "ReportClientTest_AppId";

  private static final String scanId = "ReportResourceClient_ScanId";

  private static final String reportFileName = "/" + ReportClientTest.class.getSimpleName() + "/report.zip";

  @Test
  public void testScanIdNull() {
    try {
      new ReportClient(getCLMServer().getClientConfiguration(), applicationPublicId, null /* scanId */);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage()).isEqualTo("Cannot create a ReportClient without a scanId");
    }
  }

  @Test
  public void testScanIdEmpty() {
    try {
      new ReportClient(getCLMServer().getClientConfiguration(), applicationPublicId, " " /* scanId */);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage()).isEqualTo("Cannot create a ReportClient without a scanId");
    }
  }

  @Test
  public void testLinkToReport() throws Exception {
    String appId = "app id";
    String scanId = "scan id";
    ReportClient reportClient = new ReportClient(getCLMServer().getClientConfiguration(), appId, scanId);
    UriBuilder uriBuilder = UriBuilder.fromPath(getCLMServer().getClientConfiguration().getServerUrl());
    uriBuilder.path(UserInterfaceLinksResource.RESOURCE_PATH).path(UserInterfaceLinksResource.REPORT_PATH);
    assertThat(reportClient.linkToReport()).isEqualTo(uriBuilder.build(appId, scanId).toString());
  }

  @Test
  public void testDownloadBundle() throws Exception {
    tempEntity.newApplicationWithParent(applicationPublicId);

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    mockScanReceipt(receipt);
    mockReport(scanId, reportFileName);

    File retrievedFile = tempDir.newFile();

    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    ClientScanResult clientScanResult = new ClientScanResult(tempDir.newFile(), false);
    new PolicyClient(config, applicationPublicId)
        .evaluateCLI(clientScanResult, ClientScanType.SONATYPE, new Stage(Stage.ID_BUILD));
    ReportClient client = new ReportClient(config, applicationPublicId, scanId);
    client.downloadBundle(retrievedFile);

    assertThat(retrievedFile).isFile();
    assertThat(retrievedFile.length()).isGreaterThan(0);
    // Verify that the file is in ZIP format
    new ZipFile(retrievedFile).close();
  }

  @Test
  public void testPrepareExpandedCoverageReport() throws Exception {
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    mockReport(scanId, "/" + ReportClientTest.class.getSimpleName() + "/report-expanded-coverage");

    File reportFile = new InsightWork(getCLMServer().getConfiguration()).getReportFile(app.getId(), scanId);
    assertThat(reportFile).doesNotExist();

    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    ReportClient client = new ReportClient(config, applicationPublicId, scanId);
    client.prepareExpandedCoverageReport();

    assertThat(reportFile).isFile();
  }

  @Test
  public void testUnauthorizedError() throws Exception {
    tempEntity.newApplicationWithParent(applicationPublicId);

    mockReport(scanId, reportFileName);

    File retrievedFile = tempDir.newFile();

    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(null);
    ReportClient client = new ReportClient(config, applicationPublicId, scanId);
    try {
      client.downloadBundle(retrievedFile);
      fail("Expected an HttpResponseException for Unauthorized");
    }
    catch (HttpResponseException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
    }
  }
}
