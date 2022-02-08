/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;

public class DefaultExternalTelemetryResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testPostExternalTelemetry() throws Exception {
    Map<String, String> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "SSC_INTEGRATION_METRICS");
    telemetryValues.put("ssc_integration_service_version", "1");
    telemetryValues.put("application_id", "1234-foo");
    telemetryValues.put("overwrite", "true");
    telemetryValues.put("force_upload", "false");

    HttpResponse httpResponse = restRequest()
        .header("user-agent", "my-user-agent")
        .body(telemetryValues)
        .post();

    assertResponseStatus(HttpStatus.NO_CONTENT_204, httpResponse);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.EXTERNAL_TELEMETRY_PATH);
  }
}
