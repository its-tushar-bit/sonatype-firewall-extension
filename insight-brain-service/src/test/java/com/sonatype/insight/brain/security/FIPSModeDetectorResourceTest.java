/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.security.FIPSModeDetectorResource.FIPSModeStatus;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class FIPSModeDetectorResourceTest
    extends AbstractResourceTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(FIPSModeDetectorResource.RESOURCE_PATH);
  }

  @Test
  public void testGetFIPSModeStatus_Enabled() throws Exception {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    FIPSModeStatus status = response.getBody(FIPSModeStatus.class);
    assertThat(status).isNotNull();
    assertThat(status.enabled()).isTrue();
  }

  @Test
  public void testGetFIPSModeStatus_Disabled() throws Exception {
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "false");
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    FIPSModeStatus status = response.getBody(FIPSModeStatus.class);
    assertThat(status).isNotNull();
    assertThat(status.enabled()).isFalse();
  }
}
