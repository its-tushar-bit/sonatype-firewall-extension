/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.util.EnumSet;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RepoManResourceTest
    extends AbstractResourceTest
{

  private String getServiceURL() {
    return getRestBaseUrl() + RepoManResource.SERVICE_PATH;
  }

  @Test
  public void testUploadScan() throws Exception {
    final String applicationPublicId = "RepoManResourceTest_AppId";
    final String licenseFingerprint = "RepoManResourceTest_LicenseFingerprint";
    tempEntity.newApplicationWithParent(applicationPublicId);
    setLicenseFingerprint(licenseFingerprint);

    final File saasScanFile = getScanResponseFile(licenseFingerprint);
    saasScanFile.delete();

    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("f75365d9d93b4f1ea2dd8457a25dc44d");
    scanReceipt.setTimeToReport(30L);
    saasScanFile.getParentFile().mkdirs();
    FileUtils.fileWrite(saasScanFile, "UTF-8", toJson(scanReceipt));

    final Response response = AuthedRestAccess.put(getServiceURL() + "/scan/" + applicationPublicId, "");

    assertResponseStatus(200, response);

    ScanReceipt receipt = JsonHelpers.fromJson(response.getResponseBody(), ScanReceipt.class);
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
    Response response = AuthedRestAccess.put(getServiceURL() + "/scan/unlicensedappid", "");
    assertResponseStatus(402, response);
  }

  @Test
  public void testUploadScan_EnforcementPointUnlicensed() throws Exception {
    // note these enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.Build, CLMEnforcementPoint.Develop);

    Response response = AuthedRestAccess.put(getServiceURL() + "/scan/unlicensedappid", "");
    assertResponseStatus(402, response);
  }

  @Test
  public void testUploadScan_EnforcementPointLicensed() throws Exception {
    for (CLMEnforcementPoint ep : EnumSet.of(CLMEnforcementPoint.StageRelease, CLMEnforcementPoint.Release)) {
      setEnforcementPoints(ep);
      Response response = AuthedRestAccess.put(getServiceURL() + "/scan/unknownappid", "");
      assertResponseStatus(404, response);
    }
  }
}
