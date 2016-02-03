/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

public class ProductLicenseResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testInstallUninstallLicense() throws Exception {
    installLicense();
    uninstallLicense();
  }

  @Test
  public void testInstallFailedWithNormalBrowser() throws Exception {
    getTestProductLicenseManager().forceInstallLicenseFailure(true);

    HttpResponse response = uploadLicense(licenseRequest());
    assertResponseStatus(400, response);

    assertEquals("The provided license file is invalid. Please verify you selected the correct file."
        + " If the problem persists, please contact our support team.", response.getBodyText());
  }

  @Test
  public void testInstallFailedWithIE() throws Exception {
    getTestProductLicenseManager().forceInstallLicenseFailure(true);

    // IE is expecting a 200 response back, so we need to validate the error
    HttpResponse response = uploadLicense(licenseRequest().query("noFormData", true));
    assertResponseStatus(200, response);

    assertEquals("\"The provided license file is invalid. Please verify you selected the correct file."
        + " If the problem persists, please contact our support team.\"", response.getBodyText());
  }

  @Test
  public void testInstallFailedIOError() throws Exception {
    getTestProductLicenseManager().setForceInstallIOFailure(true);

    HttpResponse response = uploadLicense(licenseRequest());
    assertResponseStatus(400, response);

    assertEquals(
        "The license file was unable to install. Please ensure server has access to "
            + System.getProperty("java.io.tmpdir") + ". If the problem persists, please contact our support team.",
        response.getBodyText());
  }

  @Test
  public void testInstall_ValidateCsrfToken() throws Exception {
    HttpResponse response = uploadLicense(licenseRequest().cookie(AntiCsrfFilter.CSRF_COOKIE_NAME, "bad-nonce"));
    assertResponseStatus(401, response);

    response = uploadLicense(licenseRequest().cookie(AntiCsrfFilter.CSRF_COOKIE_NAME, "bad-nonce").query("noFormData",
        true));
    assertResponseStatus(200, response);
    assertEquals("\"Invalid cross-site request forgery token\"", response.getBodyText());
  }

  @Test
  public void testValidateLicense() throws Exception {
    installLicense();
    HttpResponse response = restRequest().path(ProductLicenseResource.RESOURCE_PATH,
        ProductLicenseResource.VALIDATE_PATH).get();
    assertResponseStatus(200, response);
    LicenseSummary licenseSummary = response.getBody(LicenseSummary.class);
    assertThat(licenseSummary.fingerprint, is(nullValue()));
  }

  @Test
  public void testGetLicenseSummary() throws Exception {
    installLicense();
    HttpResponse response = restRequest().path(ProductLicenseResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    LicenseSummary licenseSummary = response.getBody(LicenseSummary.class);
    assertThat(licenseSummary.fingerprint, is(notNullValue()));
  }
}
