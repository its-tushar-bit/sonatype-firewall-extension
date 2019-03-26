/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiReportDataResourceV2Test
    extends AbstractResourceTest
{
  @Test
  public void testGetData_Redirect() throws Exception {
    HttpResponse response = restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .parameter("app id", "scan/id").get();

    assertResponseStatus(307, response);
    assertThat(response.getHeader("Location"))
        .isEqualTo(getRestBaseUrl() + "api/v2/applications/app%20id/reports/scan%2Fid/raw");
  }

  @Test
  public void testGetDataUrl() {
    assertThat(ApiReportDataResourceV2.getDataUrl("app id", "scan/id"))
        .isEqualTo("api/v2/applications/app%20id/reports/scan%2Fid/raw");
  }
}
