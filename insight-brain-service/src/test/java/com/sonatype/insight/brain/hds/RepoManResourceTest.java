/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.EnumSet;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.hds.RepoManResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RepoManResourceTest
    extends AbstractResourceTest
{
  private HttpRequest scanRequest(String appId) {
    return super.restRequest().path(RepoManResource.SERVICE_PATH, RepoManResource.SCAN_PATH).parameter(appId);
  }

  @Test
  public void testUploadScan() throws Exception {
    final String applicationPublicId = "RepoManResourceTest_AppId";
    final String licenseFingerprint = "RepoManResourceTest_LicenseFingerprint";
    tempEntity.newApplicationWithParent(applicationPublicId);
    setLicenseFingerprint(licenseFingerprint);

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44d");
    scanReceipt.setTimeToReport(30L);
    mockScanReceipt(scanReceipt);

    final HttpResponse response = scanRequest(applicationPublicId).put();

    assertResponseStatus(200, response);

    ScanReceipt receipt = response.getBody(ScanReceipt.class);
    assertNotNull(receipt);
    assertEquals(scanReceipt.getScanId(), receipt.getScanId());
    assertEquals(scanReceipt.getTimeToReport(), receipt.getTimeToReport());
    assertEquals("ui/links/application/RepoManResourceTest_AppId/report/f75365d9d93b4f1ea2dd8457a25dc44d",
        receipt.getReportUrl());
    assertEquals("ui/links/application/RepoManResourceTest_AppId/report/f75365d9d93b4f1ea2dd8457a25dc44d/pdf",
        receipt.getPdfUrl());
  }

  @Test
  public void testUploadScan_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = scanRequest("unlicensedappid").put();
    assertResponseStatus(402, response);
  }

  @Test
  public void testUploadScan_EnforcementPointUnlicensed() throws Exception {
    // note these enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.Build, CLMEnforcementPoint.Develop);

    HttpResponse response = scanRequest("unlicensedappid").put();
    assertResponseStatus(402, response);
  }

  @Test
  public void testUploadScan_EnforcementPointLicensed() throws Exception {
    for (CLMEnforcementPoint ep : EnumSet.of(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release)) {
      setEnforcementPoints(ep);
      HttpResponse response = scanRequest("unknownappid").put();
      assertResponseStatus(404, response);
    }
  }
}
