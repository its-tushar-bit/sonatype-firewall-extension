/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import com.sonatype.insight.brain.api.v2.ApiCrowdConfigurationResourceV2;
import com.sonatype.insight.brain.api.v2.ApiCycloneDxResourceV2;
import com.sonatype.insight.brain.api.v2.ApiEvaluationResourceV2;
import com.sonatype.insight.brain.api.v2.ApiJiraConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiLegalAttributionReportTemplateResourceV2;
import com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2;
import com.sonatype.insight.brain.api.v2.ApiMailConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiMetricsReportingResourceV2;
import com.sonatype.insight.brain.api.v2.ApiOrganizationResourceV2;
import com.sonatype.insight.brain.api.v2.ApiProxyServerConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.ApiReportResourceV2;
import com.sonatype.insight.brain.api.v2.ApiRepositoryConnectionResourceV2;
import com.sonatype.insight.brain.api.v2.ApiRepositoryIdentifiedComponentResourceV2;
import com.sonatype.insight.brain.api.v2.ApiSearchResourceV2;
import com.sonatype.insight.brain.api.v2.ApiSourceControlConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiSourceControlMetricsResource;
import com.sonatype.insight.brain.api.v2.ApiSourceControlResource;
import com.sonatype.insight.brain.api.v2.ApiThirdPartyScanResource;
import com.sonatype.insight.brain.api.v2.DefaultApiEvaluationResourceV2;
import com.sonatype.insight.brain.api.v2.DefaultApiJiraConfigurationResource;
import com.sonatype.insight.brain.api.v2.DefaultApiLegalAttributionReportTemplateResourceV2;
import com.sonatype.insight.brain.api.v2.DefaultApiLegalReportResourceV2;
import com.sonatype.insight.brain.api.v2.DefaultApiMailConfigurationResource;
import com.sonatype.insight.brain.api.v2.DefaultApiOrganizationResourceV2;
import com.sonatype.insight.brain.api.v2.DefaultApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.DefaultApiReportResourceV2;
import com.sonatype.insight.brain.api.v2.DefaultApiSearchResourceV2;
import com.sonatype.insight.brain.api.v2.DefaultApiSourceControlConfigurationResource;
import com.sonatype.insight.brain.api.v2.DefaultApiSourceControlMetricsResource;
import com.sonatype.insight.brain.api.v2.DefaultApiSourceControlResource;
import com.sonatype.insight.brain.api.v2.DefaultRepositoryConnectionResource;
import com.sonatype.insight.brain.api.v2.DefaultRepositoryIdentifiedComponentResource;
import com.sonatype.insight.brain.configuration.AutomaticApplicationsConfigurationResource;
import com.sonatype.insight.brain.configuration.AutomaticSourceControlConfigurationResource;
import com.sonatype.insight.brain.configuration.SystemNoticeResource;
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

    assertThat(underTest.isBanned(ApiEvaluationResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiEvaluationResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiJiraConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiJiraConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiLegalAttributionReportTemplateResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiLegalAttributionReportTemplateResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiLegalReportResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiLegalReportResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiCycloneDxResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiMetricsReportingResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiThirdPartyScanResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiOrganizationResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiOrganizationResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiReportDataResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiReportDataResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiReportResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiReportResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiRepositoryConnectionResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultRepositoryConnectionResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiRepositoryIdentifiedComponentResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultRepositoryIdentifiedComponentResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiSearchResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiSearchResourceV2.class)).isTrue();
    assertThat(underTest.isBanned(ApiSourceControlConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiSourceControlConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiSourceControlMetricsResource.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiSourceControlMetricsResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiSourceControlResource.class)).isTrue();
    assertThat(underTest.isBanned(DefaultApiSourceControlResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiMailConfigurationResource.class)).isFalse();
    assertThat(underTest.isBanned(DefaultApiMailConfigurationResource.class)).isFalse();
    assertThat(underTest.isBanned(LdapResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiMailConfigurationResource.class)).isFalse();
    assertThat(underTest.isBanned(ApiProxyServerConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(SystemNoticeResource.class)).isFalse();
    assertThat(underTest.isBanned(SuccessMetricsResource.class)).isTrue();
    assertThat(underTest.isBanned(AutomaticApplicationsConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(AdvancedSearchResource.class)).isTrue();
    assertThat(underTest.isBanned(AutomaticSourceControlConfigurationResource.class)).isTrue();
    assertThat(underTest.isBanned(ApiCrowdConfigurationResourceV2.class)).isTrue();
  }
}
