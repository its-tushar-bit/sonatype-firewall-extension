/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import com.sonatype.insight.brain.api.v2.ApiCrowdConfigurationResourceV2;
import com.sonatype.insight.brain.api.v2.ApiJiraConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiProxyServerConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiSearchResourceV2;
import com.sonatype.insight.brain.api.v2.ApiSourceControlConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiSourceControlMetricsResource;
import com.sonatype.insight.brain.api.v2.ApiSourceControlResource;
import com.sonatype.insight.brain.api.v2.DefaultApiJiraConfigurationResource;
import com.sonatype.insight.brain.api.v2.DefaultApiSearchResourceV2;
import com.sonatype.insight.brain.api.v2.DefaultApiSourceControlConfigurationResource;
import com.sonatype.insight.brain.api.v2.DefaultApiSourceControlMetricsResource;
import com.sonatype.insight.brain.api.v2.DefaultApiSourceControlResource;
import com.sonatype.insight.brain.configuration.AutomaticSourceControlConfigurationResource;
import com.sonatype.insight.brain.configuration.ldap.LdapResource;
import com.sonatype.insight.brain.search.AdvancedSearchResource;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsResource;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MilestoneOneBannedRestResourcesTest
{
  @Test
  public void testClassesBanned() {
    MilestoneOneBannedRestResources underTest = new MilestoneOneBannedRestResources();

    assertThat(underTest.isBanned(ApiJiraConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiJiraConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiSearchResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiSearchResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiSourceControlConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiSourceControlConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiSourceControlMetricsResource.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiSourceControlMetricsResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiSourceControlResource.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiSourceControlResource.class)).isTrue();
    assertThat(underTest.isBanned(LdapResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiProxyServerConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(SuccessMetricsResource.class)).isTrue();
    assertThat(underTest.isBanned(AdvancedSearchResource.class)).isTrue();
    assertThat(underTest.isBanned(AutomaticSourceControlConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiCrowdConfigurationResourceV2.class)).isTrue();
  }
}
