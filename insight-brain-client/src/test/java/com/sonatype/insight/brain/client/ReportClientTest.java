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
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.scan.model.ClientScanResult;
import com.sonatype.insight.scan.model.ClientScanType;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ReportClientTest
    extends AbstractBrainServiceIntegrationTest
{
  private static final String applicationPublicId = "ReportClientTest_AppId";

  private static final String scanId = "ReportResourceClient_ScanId";

  private static final String reportFileName = "/" + ReportClientTest.class.getSimpleName() + "/report.zip";

  @Test
  public void testScanIdNull() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> new ReportClient(getCLMServer().getClientConfiguration(), applicationPublicId, null /* scanId */))
        .withMessage("Cannot create a ReportClient without a scanId");
  }

  @Test
  public void testScanIdEmpty() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> new ReportClient(getCLMServer().getClientConfiguration(), applicationPublicId, " " /* scanId */))
        .withMessage("Cannot create a ReportClient without a scanId");
  }

  @Test
  public void testLinkToReport() {
    String appId = "app-id";
    String scanId = "scan-id";
    ReportClient reportClient = new ReportClient(getCLMServer().getClientConfiguration(), appId, scanId);
    UriBuilder uriBuilder = UriBuilder.fromPath(getCLMServer().getClientConfiguration().getServerUrl());
    uriBuilder.path(UserInterfaceLinksHelper.RESOURCE_PATH).path(UserInterfaceLinksHelper.REPORT_PATH);
    assertThat(reportClient.linkToReport()).isEqualTo(uriBuilder.build(appId, scanId).toString());
  }

  @Test
  public void testLinkToPrioritiesReport() {
    final String appId = "app-id";
    final String scanId = "scan-id";
    final ReportClient reportClient = new ReportClient(getCLMServer().getClientConfiguration(), appId, scanId);
    final UriBuilder uriBuilder = UriBuilder.fromPath(getCLMServer().getClientConfiguration().getServerUrl())
        .path(UserInterfaceLinksHelper.RESOURCE_PATH).path(UserInterfaceLinksHelper.PRIORITIES_PATH);
    assertThat(reportClient.linkToPrioritiesReport()).isEqualTo(uriBuilder.build(appId, scanId).toString());
  }

  @Test
  public void testLinkToIntegrationsPrioritiesReport() {
    // Given
    String appId = "app-id";
    String scanId = "scan-id";
    String integration = "bamboo";
    Configuration clientConfiguration = getCLMServer().getClientConfiguration();

    // When
    ReportClient reportClient = new ReportClient(clientConfiguration, appId, scanId);
    String actual = reportClient.linkToIntegrationPrioritiesReport(integration);

    // Then
    String expected = clientConfiguration.getServerUrl() + "ui/links/developer/integrations/app-id/scan-id/bamboo";
    assertThat(actual).isEqualTo(expected);
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
  public void testUnauthorizedError() throws Exception {
    tempEntity.newApplicationWithParent(applicationPublicId);

    mockReport(scanId, reportFileName);

    File retrievedFile = tempDir.newFile();

    Configuration config = getCLMServer().getClientConfiguration();
    config.setServerAuth(null);
    ReportClient client = new ReportClient(config, applicationPublicId, scanId);
    assertThatExceptionOfType(HttpResponseException.class).isThrownBy(() -> client.downloadBundle(retrievedFile))
        .withMessage(ErrorResponseGenerator.MSG_MISSING_CREDENTIALS);
  }
}
