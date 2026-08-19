/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.product.license} package to access the package-private
 * {@code ProductLicenseResource.VALIDATE_PATH} that the legacy {@code ProductLicenseResourceAuthzTest} used.
 */
@IqH2Test
class IqH2ProductLicenseResourceAuthzTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().anon();
  }

  @Test
  void testValidateLicense() throws Exception {
    // no authentication required
    HttpRequest request = restRequest().path(ProductLicenseResource.RESOURCE_PATH)
        .path(ProductLicenseResource.VALIDATE_PATH);
    HttpResponse response = request.anon().get();
    assertThat(response.getStatusCode()).isEqualTo(200);
  }
}
