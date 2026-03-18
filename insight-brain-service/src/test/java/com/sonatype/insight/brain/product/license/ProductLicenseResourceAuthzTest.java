/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ProductLicenseResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testValidateLicense() throws Exception {
    // no authentication required
    HttpRequest request = restRequest().path(ProductLicenseResource.RESOURCE_PATH)
        .path(ProductLicenseResource.VALIDATE_PATH);
    HttpResponse response = request.anon().get();
    assertThat(response.getStatusCode()).isEqualTo(200);
  }
}
