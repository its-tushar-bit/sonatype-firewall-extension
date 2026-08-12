/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.spring.config.SecurityConfiguration;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import java.net.HttpCookie;
import java.util.Arrays;
import org.junit.Test;

public class TelemetryResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testForwardFrontendTelemetryToHds() throws Exception {
    TelemetryData postData = new TelemetryData(TelemetryPurpose.GETTING_STARTED_USAGE, 1L);
    postData.getAttributes().put("attr1", "value1");
    postData.getAttributes().put("attr2", Arrays.asList("value2", "value3", 4));

    // The telemetry endpoint requires a user session, which it doesn't create itself. So we need to login first
    HttpResponse loginResponse = restRequest().path("rest/user/session").post();
    HttpCookie sessionCookie = loginResponse.getCookie(SecurityConfiguration.SESSION_COOKIE_NAME);

    HttpRequest request = restRequest().path(TelemetryResource.RESOURCE_PATH).body(postData).cookie(sessionCookie);

    HttpResponse response = request.post();

    assertResponseStatus(204, response);
  }
}
