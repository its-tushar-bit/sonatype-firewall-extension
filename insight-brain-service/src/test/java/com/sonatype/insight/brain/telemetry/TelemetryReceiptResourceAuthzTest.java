/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class TelemetryReceiptResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Before
  public void setUp() {
    grantConfigureSystemPermission();
  }

  @Test
  public void testGetReceipts_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH)
        .anon()
        .get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testGetReceipts_authz() throws Exception {
    HttpRequest request = restRequest().path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH);
    testAuthzGet(request, 200);
  }

  @Test
  public void testEnableReceipts_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH, "enable")
        .anon()
        .get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testEnableReceipts_authz() throws Exception {
    HttpRequest request = restRequest().path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH, "enable");
    testAuthzGet(request, 200);
  }

  @Test
  public void testDisableReceipts_Unauthenticated() throws Exception {
    HttpResponse response = restRequest()
        .path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH, "disable")
        .anon()
        .get();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void testDisableReceipts_authz() throws Exception {
    HttpRequest request = restRequest().path(TelemetryReceiptResource.TELEMETRY_RECEIPTS_PATH, "disable");
    testAuthzGet(request, 200);
  }
}
