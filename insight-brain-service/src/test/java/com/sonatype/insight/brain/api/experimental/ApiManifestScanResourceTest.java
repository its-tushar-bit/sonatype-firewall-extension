/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;

import static com.sonatype.insight.brain.api.experimental.ApiManifestScanResource.RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiManifestScanResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testManifestScan() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    // when application manifest is scanned
    HttpResponse response = restRequest().path(RESOURCE_PATH).parameter(app.getId()).get();

    // the response always is no content
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT_204);
  }
}
