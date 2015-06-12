/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class CIResourceTest
    extends AbstractResourceTest
{
  private HttpRequest scanRequest(String appId) {
    return restRequest().path(CIResource.SERVICE_PATH, CIResource.SCAN_PATH).parameter(appId);
  }

  @Test
  public void testScan() throws Exception {
    final String applicationPublicId = "CIResourceTest_AppId";
    final String licenseFingerprint = "CIResourceTest_LicenseFingerprint";
    tempEntity.newApplicationWithParent(applicationPublicId);
    setLicenseFingerprint(licenseFingerprint);

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44d");
    scanReceipt.setTimeToReport(30L);
    mockScanReceipt(scanReceipt);

    final Response response = scanRequest(applicationPublicId).put();

    assertResponseStatus(200, response);

    ScanReceipt receipt = fromJson(response, ScanReceipt.class);
    assertThat(receipt, is(notNullValue()));
    assertThat(receipt.getScanId(), is(scanReceipt.getScanId()));
    assertThat(receipt.getTimeToReport(), is(scanReceipt.getTimeToReport()));
    assertThat(receipt.getReportUrl(),
        is("ui/links/application/" + applicationPublicId + "/report/" + receipt.getScanId()));
    assertThat(receipt.getPdfUrl(), is("ui/links/application/" + applicationPublicId + "/report/" + receipt.getScanId()
        + "/pdf"));
  }

  @Test
  public void testScan_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = scanRequest("unlicensedapp").put();
    assertResponseStatus(402, response);
  }

  @Test
  public void testScan_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = scanRequest("unlicensedapp").put();
    assertResponseStatus(402, response);
  }
}
