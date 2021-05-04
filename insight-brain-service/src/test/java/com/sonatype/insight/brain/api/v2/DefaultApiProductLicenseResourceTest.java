/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultApiProductLicenseResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH);
  }

  @Test
  public void testInstallUninstallLicense() throws Exception {
    installLicense();
    uninstallLicense();
  }

  @Test
  public void testInstall_FailedNoContent() throws Exception {
    HttpResponse response = uploadLicense(licenseRequest(new byte[0]));

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("The provided license file sonatype.lic is invalid. Please verify you selected the correct file."
            + " If the problem persists, please contact our support team.");
  }

  @Test
  public void testInstall_FailedIOError() throws Exception {
    getTestProductLicenseManager().setForceInstallIOFailure(true);

    HttpResponse response = uploadLicense(licenseRequest());

    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("The license file sonatype.lic was unable to install. Please ensure server has access to "
            + System.getProperty("java.io.tmpdir") + ". If the problem persists, please contact our support team.");
  }
}
