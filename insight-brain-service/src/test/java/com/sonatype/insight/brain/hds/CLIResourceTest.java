/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;

import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.scan.model.ClientScanType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CLIResourceTest
    extends AbstractScanResourceTest
{
  @Override
  protected HttpRequest scanRequest(String appId) {
    return restRequest().path(CLIResource.RESOURCE_PATH, CLIResource.SCAN_PATH).parameter(appId);
  }

  @Test
  public void testPutScan_ExpandedCoverageScan() throws Exception {
    String applicationPublicId = "TestAppId";
    String licenseFingerprint = "TestLicenseFingerprint";
    tempEntity.newApplicationWithParent(applicationPublicId);
    setLicenseFingerprint(licenseFingerprint);

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44a");
    scanReceipt.setTimeToReport(30L);
    mockScanReceipt(scanReceipt);

    HttpResponse response = scanRequest(applicationPublicId).query("scanType", ClientScanType.EXPANDED_COVERAGE)
        .body("test scan file content", MediaType.APPLICATION_OCTET_STREAM).put();
    assertResponseStatus(200, response);

    ScanReceipt receipt = response.getBody(ScanReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getScanId()).isEqualTo(scanReceipt.getScanId());
    assertThat(receipt.getTimeToReport()).isEqualTo(scanReceipt.getTimeToReport());
    assertThat(receipt.getReportUrl())
        .isEqualTo("ui/links/application/" + applicationPublicId + "/report/" + receipt.getScanId());
    assertThat(receipt.getPdfUrl())
        .isEqualTo("ui/links/application/" + applicationPublicId + "/report/" + receipt.getScanId() + "/pdf");
  }

  @Test
  public void testPutScan_TwistlockScan() throws Exception {
    String applicationPublicId = "TestAppId";
    String licenseFingerprint = "TestLicenseFingerprint";
    tempEntity.newApplicationWithParent(applicationPublicId);
    setLicenseFingerprint(licenseFingerprint);

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44d");
    scanReceipt.setTimeToReport(30L);
    mockScanReceipt(scanReceipt);

    File inputScanFile = TwistlockScanTestHelper.createInputScanFile(tempDir,
        new File("target/test-classes/CLIResourceTest/TwistlockScan"));
    HttpResponse response = scanRequest(applicationPublicId).query("scanType", ClientScanType.TWISTLOCK)
        .body(inputScanFile, MediaType.APPLICATION_OCTET_STREAM).put();

    assertResponseStatus(200, response);

    ScanReceipt receipt = response.getBody(ScanReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getScanId()).isEqualTo(scanReceipt.getScanId());
    assertThat(receipt.getTimeToReport()).isEqualTo(scanReceipt.getTimeToReport());
    assertThat(receipt.getReportUrl())
        .isEqualTo("ui/links/application/" + applicationPublicId + "/report/" + receipt.getScanId());
    assertThat(receipt.getPdfUrl())
        .isEqualTo("ui/links/application/" + applicationPublicId + "/report/" + receipt.getScanId() + "/pdf");
  }
}
