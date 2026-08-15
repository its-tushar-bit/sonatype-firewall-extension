/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.product.license} package because it references the package-private
 * {@link ProductLicenseResource#VALIDATE_PATH}.
 */
@IqH2Test
class IqH2ProductLicenseResourceTest
{
  private IqTestContext ctx;

  @Test
  void testValidateLicense() throws Exception {
    ctx.installLicense();
    HttpResponse response = ctx.restRequest()
        .path(ProductLicenseResource.RESOURCE_PATH,
            ProductLicenseResource.VALIDATE_PATH)
        .get();
    ctx.assertResponseStatus(200, response);
    LicenseSummary licenseSummary = response.getBody(LicenseSummary.class);
    assertThat(licenseSummary.productEdition).isNotNull();
  }

  @Test
  void testGetLicenseInfo() throws Exception {
    ctx.installLicense();
    HttpResponse response = ctx.restRequest().path(ProductLicenseResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    LicenseInfo licenseInfo = response.getBody(LicenseInfo.class);
    assertThat(licenseInfo.fingerprint).isNotNull();
  }
}
