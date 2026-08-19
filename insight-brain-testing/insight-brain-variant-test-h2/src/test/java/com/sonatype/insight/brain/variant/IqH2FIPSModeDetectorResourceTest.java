/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.security.FIPSModeDetectorResource;
import com.sonatype.insight.brain.security.FIPSModeDetectorResource.FIPSModeStatus;
import com.sonatype.insight.brain.security.TestEnvironmentVariables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2FIPSModeDetectorResourceTest
{
  private IqTestContext ctx;

  private final TestEnvironmentVariables environmentVariables = new TestEnvironmentVariables();

  @AfterEach
  void tearDown() {
    environmentVariables.restore();
  }

  @Test
  void testGetFIPSModeStatus_Enabled() throws Exception {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    HttpResponse response = ctx.restRequest().path(FIPSModeDetectorResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    FIPSModeStatus status = response.getBody(FIPSModeStatus.class);
    assertThat(status).isNotNull();
    assertThat(status.enabled()).isTrue();
  }

  @Test
  void testGetFIPSModeStatus_Disabled() throws Exception {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");
    HttpResponse response = ctx.restRequest().path(FIPSModeDetectorResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    FIPSModeStatus status = response.getBody(FIPSModeStatus.class);
    assertThat(status).isNotNull();
    assertThat(status.enabled()).isFalse();
  }
}
