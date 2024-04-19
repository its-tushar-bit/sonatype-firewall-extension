/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalResource;
import com.sonatype.insight.brain.api.v2.ApiAdvancedSearchResourceV2;
import com.sonatype.insight.brain.api.v2.ApiJiraConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiLegalAttributionReportTemplateResourceV2;
import com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2;
import com.sonatype.insight.brain.api.v2.ApiProxyServerConfigurationResource;
import com.sonatype.insight.brain.configuration.ldap.LdapResource;
import com.sonatype.insight.brain.labs.LabsResource;
import com.sonatype.insight.brain.search.AdvancedSearchResource;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsResource;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TemporarilyBannedRestResourcesTest
{
  @Test
  public void testClassesBanned() {
    TemporarilyBannedRestResources underTest = new TemporarilyBannedRestResources();

    assertThat(underTest.isBanned(LdapResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiProxyServerConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(SuccessMetricsResource.class)).isTrue();

    assertThat(underTest.isBanned(AdvancedSearchResource.class)).isFalse();
    assertThat(underTest.isBanned(ApiAdvancedSearchResourceV2.class)).isFalse();

    assertThat(underTest.isBanned(LabsResource.class)).isTrue();

    assertThat(underTest.isBanned(ApiJiraConfigurationResource.class)).isTrue();

    assertThat(underTest.isBanned(ApiLicenseLegalResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiLegalAttributionReportTemplateResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiLegalReportResourceV2.class)).isTrue();
  }
}
