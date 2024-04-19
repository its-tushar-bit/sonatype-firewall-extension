/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesResource;
import com.sonatype.insight.brain.api.v2.ApiCrowdConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiDataRetentionPolicyResource;
import com.sonatype.insight.brain.api.v2.ApiSourceControlConfigurationResource;
import com.sonatype.insight.brain.support.SupportResource;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PermanentlyBannedRestResourcesTest
{
  @Test
  public void testClassesBanned() {
    PermanentlyBannedRestResources underTest = new PermanentlyBannedRestResources();

    assertThat(underTest.isBanned(ApiCrowdConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiDataRetentionPolicyResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiConfigFeaturesResource.class)).isTrue();
    assertThat(underTest.isBanned(SupportResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiSourceControlConfigurationResource.class)).isTrue();
  }
}
