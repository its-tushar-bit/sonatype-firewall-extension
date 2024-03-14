/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesResource;
import com.sonatype.insight.brain.api.v2.ApiCrowdConfigurationResourceV2;
import com.sonatype.insight.brain.api.v2.ApiDataRetentionPolicyResource;
import com.sonatype.insight.brain.api.v2.ApiExternalTelemetryResourceV2;
import com.sonatype.insight.brain.api.v2.ApiSourceControlConfigurationResource;
import com.sonatype.insight.brain.api.v2.DefaultApiCrowdConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiDataRetentionPolicyResource;
import com.sonatype.insight.brain.api.v2.DefaultExternalTelemetryResource;
import com.sonatype.insight.brain.support.SupportResource;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PermanentlyBannedRestResourcesTest
{
  @Test
  public void testClassesBanned() {
    PermanentlyBannedRestResources underTest = new PermanentlyBannedRestResources();

    assertThat(underTest.isBanned(ApiCrowdConfigurationResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiCrowdConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiDataRetentionPolicyResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiDataRetentionPolicyResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiExternalTelemetryResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultExternalTelemetryResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiConfigFeaturesResource.class)).isTrue();
    assertThat(underTest.isBanned(SupportResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiSourceControlConfigurationResource.class)).isTrue();
  }
}
