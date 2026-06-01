/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class NexusOneIndexAccessFilterIntegrationTest
    extends AbstractBrainServiceIntegrationTest
{
  @After
  public void tearDownPreviewFlag() {
    tempEntity.deleteSystemConfigurationProperty(
        SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.getPropertyName());
  }

  @Test
  public void anonymousRequest_flagOff_redirectsToClassicIndex() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = restRequest()
        .path(NexusOneIndexAccessFilter.URL_PATTERN)
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(302);
    assertThat(response.getHeader("Location")).contains("/assets/index.html");
    assertThat(response.getHeader("Location")).doesNotContain("nexus-one");
  }

  @Test
  public void authenticatedRequest_flagOff_redirectsToClassicIndex() throws Exception {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);

    HttpResponse response = restRequest()
        .path(NexusOneIndexAccessFilter.URL_PATTERN)
        .auth()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(302);
    assertThat(response.getHeader("Location")).contains("/assets/index.html");
  }
}
