/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.license.LicenseResource;
import com.sonatype.insight.brain.model.license.License;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2LicenseResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(LicenseResource.RESOURCE_PATH);
  }

  @Test
  void testGet() throws Exception {
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    License[] licenses = response.getBody(License[].class);
    assertThat(licenses).extracting(License::getId)
        .contains(License.NO_SOURCE_LICENSE_ID, License.NOT_DECLARED_ID,
            License.NO_SOURCES_ID, License.NOT_SUPPORTED_ID);
  }

  @Test
  void testGet_FilterSynthetic() throws Exception {
    HttpResponse response = restRequest().query("filterSynthetic", true).get();
    ctx.assertResponseStatus(200, response);

    License[] licenses = response.getBody(License[].class);
    assertThat(licenses).extracting(License::getId)
        .isNotEmpty()
        .doesNotContain(License.NO_SOURCE_LICENSE_ID,
            License.NOT_DECLARED_ID, License.NO_SOURCES_ID, License.NOT_SUPPORTED_ID);
  }
}
