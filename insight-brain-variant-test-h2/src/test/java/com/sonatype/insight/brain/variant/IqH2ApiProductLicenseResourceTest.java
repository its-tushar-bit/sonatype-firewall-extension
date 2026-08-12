/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.PublicApiPaths;

import org.junit.jupiter.api.Test;
import org.sonatype.licensing.product.ProductLicenseManager;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiProductLicenseResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH);
  }

  private HttpRequest licenseRequest() {
    return licenseRequest(new byte[1]);
  }

  private HttpRequest licenseRequest(Object licenseFile) {
    return ctx.restRequest()
        .path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH)
        .part("file", "sonatype.lic", licenseFile);
  }

  private HttpResponse uploadLicense(HttpRequest licenseRequest) throws Exception {
    return licenseRequest.post();
  }

  private TestProductLicenseManager getTestProductLicenseManager() {
    return (TestProductLicenseManager) ctx.lookup(ProductLicenseManager.class);
  }

  @Test
  void testInstallUninstallLicense() throws Exception {
    ctx.installLicense();
    ctx.uninstallLicense();
  }

  @Test
  void testInstall_FailedNoContent() throws Exception {
    HttpResponse response = uploadLicense(licenseRequest(new byte[0]));

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("The provided license file sonatype.lic is invalid. Please verify you selected the correct file."
            + " If the problem persists, please contact our support team.");
  }

  @Test
  void testInstall_FailedIOError() throws Exception {
    getTestProductLicenseManager().setForceInstallIOFailure(true);
    try {
      HttpResponse response = uploadLicense(licenseRequest());

      ctx.assertResponseStatus(400, response);
      assertThat(response.getBodyText())
          .isEqualTo("The license file sonatype.lic was unable to install. Please ensure server has access to "
              + System.getProperty("java.io.tmpdir") + ". If the problem persists, please contact our support team.");
    }
    finally {
      getTestProductLicenseManager().setForceInstallIOFailure(false);
    }
  }
}
