/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

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
    tempEntity.newApplicationWithParent(applicationPublicId);

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
}
