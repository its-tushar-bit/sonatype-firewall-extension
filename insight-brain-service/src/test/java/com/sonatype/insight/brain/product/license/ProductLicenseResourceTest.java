/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    HttpResponse response = uploadLicense(licenseRequest(new byte[0]));
    assertResponseStatus(400, response);

    assertThat(response.getBodyText())
        .isEqualTo("The provided license file sonatype.lic is invalid. Please verify you selected the correct file."
            + " If the problem persists, please contact our support team.");
  }

  @Test
  public void testInstallFailedWithIE() throws Exception {
    // IE is expecting a 200 response back, so we need to validate the error
    HttpResponse response = uploadLicense(licenseRequest(new byte[0]).query("noFormData", true));
    assertResponseStatus(200, response);

    assertThat(response.getBodyText())
        .isEqualTo("\"The provided license file sonatype.lic is invalid. Please verify you selected the correct file."
            + " If the problem persists, please contact our support team.\"");
  }

  @Test
  public void testInstallFailedIOError() throws Exception {
    getTestProductLicenseManager().setForceInstallIOFailure(true);

    HttpResponse response = uploadLicense(licenseRequest());
    assertResponseStatus(400, response);

    assertThat(response.getBodyText())
        .isEqualTo("The license file sonatype.lic was unable to install. Please ensure server has access to "
            + System.getProperty("java.io.tmpdir") + ". If the problem persists, please contact our support team.");
  }

  @Test
  public void testInstall_ValidateCsrfToken() throws Exception {
    HttpResponse response = uploadLicense(licenseRequest().cookie(AntiCsrfFilter.CSRF_COOKIE_NAME, "bad-nonce"));
    assertResponseStatus(401, response);

    response = uploadLicense(licenseRequest().cookie(AntiCsrfFilter.CSRF_COOKIE_NAME, "bad-nonce").query("noFormData",
        true));
    assertResponseStatus(200, response);
    assertThat(response.getBodyText()).isEqualTo("\"Invalid cross-site request forgery token\"");
  }

  @Test
  public void testValidateLicense() throws Exception {
    installLicense();
    HttpResponse response = restRequest().path(ProductLicenseResource.RESOURCE_PATH,
        ProductLicenseResource.VALIDATE_PATH).get();
    assertResponseStatus(200, response);
    LicenseSummary licenseSummary = response.getBody(LicenseSummary.class);
    assertThat(licenseSummary.productEdition).isNotNull();
  }

  @Test
  public void testGetLicenseInfo() throws Exception {
    installLicense();
    HttpResponse response = restRequest().path(ProductLicenseResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    LicenseInfo licenseInfo = response.getBody(LicenseInfo.class);
    assertThat(licenseInfo.fingerprint).isNotNull();
  }
}
