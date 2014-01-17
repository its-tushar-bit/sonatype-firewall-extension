/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.File;
import java.net.URL;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import com.ning.http.client.Response;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

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
    Response response = AuthedRestAccess.get(getServiceURL() + "/validate/" + applicationPublicId);
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), equalTo("OK"));
    applicationDAO.getByPublicIdNotNull(applicationPublicId);

    // Validation should not fail if the application exists
    response = AuthedRestAccess.get(getServiceURL() + "/validate/" + applicationPublicId);
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), equalTo("OK"));
    applicationDAO.getByPublicIdNotNull(applicationPublicId);

    applicationDAO.delete(application);

    // validate service always returns 200, the actual result is in the response body
    response = AuthedRestAccess.get(getServiceURL() + "/validate/" + applicationPublicId);
    assertResponseStatus(200, response);
    assertThat(response.getResponseBody(), equalTo("Invalid application id " + applicationPublicId));
  }

  @Test
  public void testValidate_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getServiceURL() + "/validate/unlicensedapp");
    assertResponseStatus(402, response);
  }

  @Test
  public void testValidate_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = AuthedRestAccess.get(getServiceURL() + "/validate/unlicensedapp");
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

    final Response response = AuthedRestAccess.put(getServiceURL() + "/scan/" + applicationPublicId, "");

    assertResponseStatus(200, response);

    assertThat(response.getResponseBody(), equalToIgnoringWhiteSpace(FileUtils.fileRead(saasScanFile, "UTF-8")));
  }

  @Test
  public void testScan_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.put(getServiceURL() + "/scan/unlicensedapp", "");
    assertResponseStatus(402, response);
  }

  @Test
  public void testScan_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = AuthedRestAccess.put(getServiceURL() + "/scan/unlicensedapp", "");
    assertResponseStatus(402, response);
  }

  private String getServiceURL() {
    return getRestBaseUrl() + CIResource.SERVICE_PATH;
  }
}
