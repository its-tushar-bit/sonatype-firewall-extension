/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.landing.NexusOneIndexAccessFilter;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2NexusOneIndexAccessFilterIntegrationTest
{
  private IqTestContext ctx;

  @AfterEach
  void tearDownPreviewFlag() {
    ctx.tempEntity()
        .deleteSystemConfigurationProperty(
            SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
  }

  @Test
  void anonymousRequest_flagOff_redirectsToClassicIndex() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = ctx.restRequest()
        .path(NexusOneIndexAccessFilter.URL_PATTERN)
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(302);
    assertThat(response.getHeader("Location")).contains("/assets/index.html");
    assertThat(response.getHeader("Location")).doesNotContain("nexus-one");
  }

  @Test
  void authenticatedRequest_flagOff_redirectsToClassicIndex() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = ctx.restRequest()
        .path(NexusOneIndexAccessFilter.URL_PATTERN)
        .auth()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(302);
    assertThat(response.getHeader("Location")).contains("/assets/index.html");
  }
}
