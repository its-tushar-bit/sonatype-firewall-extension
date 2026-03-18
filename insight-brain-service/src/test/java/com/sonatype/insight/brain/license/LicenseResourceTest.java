/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(LicenseResource.RESOURCE_PATH);
  }

  @Test
  public void testGet() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    License[] licenses = response.getBody(License[].class);
    assertThat(licenses).extracting(License::getId)
        .contains(License.NO_SOURCE_LICENSE_ID, License.NOT_DECLARED_ID,
            License.NO_SOURCES_ID, License.NOT_SUPPORTED_ID);
  }

  @Test
  public void testGet_FilterSynthetic() throws Exception {
    HttpResponse response = restRequest().query("filterSynthetic", true).get();
    assertResponseStatus(200, response);

    License[] licenses = response.getBody(License[].class);
    assertThat(licenses).extracting(License::getId)
        .isNotEmpty()
        .doesNotContain(License.NO_SOURCE_LICENSE_ID,
            License.NOT_DECLARED_ID, License.NO_SOURCES_ID, License.NOT_SUPPORTED_ID);
  }
}
