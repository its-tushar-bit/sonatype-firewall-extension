/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductLicenseResourceTest
    extends AbstractResourceTest
{
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
