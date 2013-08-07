/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.test.RestAccess;

public class CIResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testValidate() throws Exception {
    final String applicationPublicId = "CIResourceTest_testValidate_AppId";
    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application application = applicationDAO.getByPublicId(applicationPublicId);
    Assert.assertNull(application);

    application = new Application();
    application.setPublicId(applicationPublicId);
    application.setName("CIResourceTest-Application-Name");
    applicationDAO.insert(application);

    // Validate that the application was created
    Response response = RestAccess.get(getServiceURL() + "/validate/" + applicationPublicId);
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), equalTo("OK"));
    applicationDAO.getByPublicIdNotNull(applicationPublicId);

    // Validation should not fail if the application exists
    response = RestAccess.get(getServiceURL() + "/validate/" + applicationPublicId);
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), equalTo("OK"));
    applicationDAO.getByPublicIdNotNull(applicationPublicId);

    applicationDAO.delete(application);

    // validate service always returns 200, the actual result is in the response body
    response = RestAccess.get(getServiceURL() + "/validate/" + applicationPublicId);
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), equalTo("Invalid application id " + applicationPublicId));
  }

  @Test
  public void testValidate_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = RestAccess.get(getServiceURL() + "/validate/unlicensedapp");
    assertResponseStatus(402, response);
  }

  @Test
  public void testValidate_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = RestAccess.get(getServiceURL() + "/validate/unlicensedapp");
    assertResponseStatus(402, response);
  }

  @Test
  public void testScan() throws Exception {
    final String applicationPublicId = "CIResourceTest_AppId";
    final String licenseFingerprint = "CIResourceTest_LicenseFingerprint";
    createApplication(applicationPublicId);
    setLicenseFingerprint(licenseFingerprint);

    final File saasScanFile = getScanResponseFile(licenseFingerprint);
    saasScanFile.delete();

    final URL testScanResultUrl = getClass().getResource("/CIResourceTest/scan.json");
    FileUtils.copyFile(new File(testScanResultUrl.getFile()), saasScanFile);

    final Response response = RestAccess.put(getServiceURL() + "/scan/" + applicationPublicId, "");

    assertResponseStatus(200, response);

    assertThat(response.getResponseBody(), equalToIgnoringWhiteSpace(FileUtils.fileRead(saasScanFile, "UTF-8")));
  }

  @Test
  public void testScan_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = RestAccess.put(getServiceURL() + "/scan/unlicensedapp", "");
    assertResponseStatus(402, response);
  }

  @Test
  public void testScan_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = RestAccess.put(getServiceURL() + "/scan/unlicensedapp", "");
    assertResponseStatus(402, response);
  }

  @Test
  public void testReport() throws Exception {
    final String applicationPublicId = "CIResourceTest_AppId";
    createApplication(applicationPublicId);
    final String scanId = "CIResourceTest_ScanId";
    final String licenseFingerprint = "CIResourceTest_LicenseFingerprint";
    setLicenseFingerprint(licenseFingerprint);

    final File saasReportFile = getReportResponseFile(licenseFingerprint, scanId);
    saasReportFile.delete();

    final URL testReportResultUrl = getClass().getResource("/CIResourceTest/report.zip");
    FileUtils.copyFile(new File(testReportResultUrl.getFile()), saasReportFile);

    final Response response = RestAccess.get(getServiceURL() + "/report/" + applicationPublicId + "?scanId=" + scanId);

    assertResponseStatus(200, response);

    assertThat(IOUtil.toByteArray(response.getResponseBodyAsStream()),
        equalTo(IOUtil.toByteArray(testReportResultUrl.openStream())));
  }

  @Test
  public void testReport_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = RestAccess.get(getServiceURL() + "/report/unlicensedapp?scanId=unlicensedscanid");
    assertResponseStatus(402, response);
  }

  @Test
  public void testReport_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = RestAccess.get(getServiceURL() + "/report/unlicensedapp?scanId=unlicensedscanid");
    assertResponseStatus(402, response);
  }

  @Test
  public void testArtifact() throws Exception {
    final String scanId = "CIResourceTest_ScanId";

    final String query = scanId + "?groupId=org.springframework&artifactId=spring-core&version=2.5.6";
    Response response = RestAccess.get(getServiceURL() + "/artifact/" + query);
    assertResponseStatus(307, response);

    response = RestAccess.get(response.getHeader("Location"));
    assertResponseStatus(200, response);

    assertThat(response.getResponseBody(), stringContainsInOrder(Arrays.asList("\"groupId\"",
        "\"org.springframework\"", "\"artifactId\"", "\"spring-core\"", "\"version\"", "\"2.5.6\"")));
  }

  @Test
  public void testArtifact_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = RestAccess.get(getServiceURL()
        + "/artifact/unlicensedscanid?groupId=ulg&artifactId=ula&version=ulv");
    assertResponseStatus(402, response);
  }

  @Test
  public void testArtifact_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = RestAccess.get(getServiceURL()
        + "/artifact/unlicensedscanid?groupId=ulg&artifactId=ula&version=ulv");
    assertResponseStatus(402, response);
  }

  private String getServiceURL() {
    return getRestBaseUrl() + CIResource.SERVICE_PATH;
  }
}
