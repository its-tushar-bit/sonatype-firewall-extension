/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @deprecated The tested class is deprecated
 */
@Deprecated
public class DeprecatedCLIResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testPutScan() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44d");
    scanReceipt.setTimeToReport(30L);
    mockScanReceipt(scanReceipt);

    String testClientUserAgent = "testClientUserAgent";
    HttpRequest request = scanRequest(app.getPublicId());
    request.header(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, testClientUserAgent);
    final HttpResponse response = request.put();

    assertResponseStatus(200, response);

    ScanReceipt receipt = response.getBody(ScanReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getScanId()).isEqualTo(scanReceipt.getScanId());
    assertThat(receipt.getTimeToReport()).isEqualTo(scanReceipt.getTimeToReport());
    assertThat(receipt.getReportUrl())
        .isEqualTo("ui/links/application/" + app.getPublicId() + "/report/" + receipt.getScanId());
    assertThat(receipt.getPdfUrl())
        .isEqualTo("ui/links/application/" + app.getPublicId() + "/report/" + receipt.getScanId() + "/pdf");

    assertThat(getHdsServer().getCapturedRequestHttpHeaders(ScanUploader.HDS_PATH)
        .get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testPutScan_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = scanRequest("unlicensedapp").put();
    assertResponseStatus(402, response);
  }

  @Test
  public void testPutScan_FeatureUnlicensed() throws Exception {
    setMissingFeature(LicensedFeature.CLI_INTEGRATION);

    HttpResponse response = scanRequest("unlicensedapp").put();
    assertResponseStatus(402, response);
  }

  private HttpRequest scanRequest(String appId) {
    return restRequest().path(DeprecatedCLIResource.RESOURCE_PATH, DeprecatedCLIResource.SCAN_PATH).parameter(appId);
  }
}
