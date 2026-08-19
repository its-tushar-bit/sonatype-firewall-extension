/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiExternalTelemetryResourceTest
{
  private IqTestContext ctx;

  @Test
  void testPostExternalTelemetry() throws Exception {
    Map<String, Object> telemetryValues = new HashMap<>();
    telemetryValues.put("telemetry_purpose", "SYNC_SERVICE_METRICS");
    telemetryValues.put("violation_count", 1);
    telemetryValues.put("waiver_count", 12);

    HttpResponse httpResponse = restRequest()
        .header("user-agent", "SSC_Sync_Service/5.0.2 (Java 1.8.0_352; Mac OS X 10.16)")
        .header("X-CLM-Client-Instance-Id", "bf1809f09dad40ec86f1e13daf96fe8c")
        .body(telemetryValues)
        .post();

    ctx.assertResponseStatus(HttpStatus.NO_CONTENT_204, httpResponse);
  }

  @Test
  void testPostExternalTelemetry_NotAuthenticated() throws Exception {
    HttpResponse httpResponse = restRequest().anon().post();
    ctx.assertResponseStatus(HttpStatus.UNAUTHORIZED_401, httpResponse);
    assertThat(httpResponse.getBodyText()).isEqualTo("Missing credentials.");
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.EXTERNAL_TELEMETRY_PATH);
  }
}
