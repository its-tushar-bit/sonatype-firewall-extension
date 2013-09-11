/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.net.URL;
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
    createApplication(applicationPublicId);
    setLicenseFingerprint(licenseFingerprint);

    final File saasScanFile = getScanResponseFile(licenseFingerprint);
    saasScanFile.delete();

    final URL testScanResultUrl = getClass().getResource("/RepoManResourceTest/scan.json");
    FileUtils.copyFile(new File(testScanResultUrl.getFile()), saasScanFile);

    final Response response = AuthedRestAccess.put(getServiceURL() + "/scan/" + applicationPublicId, "");

    assertResponseStatus(200, response);

    ScanReceipt scanReceipt = JsonHelpers.fromJson(response.getResponseBody(), ScanReceipt.class);
    assertNotNull(scanReceipt);
    assertEquals("f75365d9d93b4f1ea2dd8457a25dc44d", scanReceipt.getScanId());
    assertEquals(Long.valueOf(30), scanReceipt.getTimeToReport());
    assertEquals("rest/report/RepoManResourceTest_AppId/f75365d9d93b4f1ea2dd8457a25dc44d/embedReport/",
        scanReceipt.getReportUrl());
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
    setEnforcementPoints(CLMEnforcementPoint.Build, CLMEnforcementPoint.Develop, CLMEnforcementPoint.Procure);

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
