/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import java.util.List;

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
import com.sonatype.insight.brain.configuration.AutomaticApplicationsConfigurationResource;
import com.sonatype.insight.brain.configuration.AutomaticSourceControlConfigurationResource;
import com.sonatype.insight.brain.configuration.SystemNoticeResource;
import com.sonatype.insight.brain.configuration.ldap.LdapResource;
import com.sonatype.insight.brain.configuration.webhook.WebhookResource;
import com.sonatype.insight.brain.search.AdvancedSearchResource;
import com.sonatype.insight.brain.service.banning.BannedImplementation;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsResource;

import com.google.common.collect.ImmutableList;

/**
 * It was determined as part of CLM-23906, CLM-23907 that certain REST resources are not going to be supported for
 * Milestone 1, mostly because they relate to Lifecycle functionality and are not needed to support Firewall. This class
 * encapsulates those REST resources and is expected to change as we test and adapt each Resource to be MTIQ supported.
 */
public class MilestoneOneBannedRestResources
    implements BannedImplementation
{
  private static final List<Class> BANNED_REST_RESOURCES = ImmutableList.of(
      ApiEvaluationResourceV2.class,
      ApiJiraConfigurationResource.class,
      ApiLegalAttributionReportTemplateResourceV2.class,
      ApiLegalReportResourceV2.class,
      ApiCycloneDxResourceV2.class,
      ApiMetricsReportingResourceV2.class,
      ApiThirdPartyScanResource.class,
      ApiOrganizationResourceV2.class,
      ApiReportDataResourceV2.class,
      ApiReportResourceV2.class,
      ApiRepositoryConnectionResourceV2.class,
      ApiRepositoryIdentifiedComponentResourceV2.class,
      ApiSearchResourceV2.class,
      ApiSourceControlConfigurationResource.class,
      ApiSourceControlMetricsResource.class,
      ApiSourceControlResource.class,
      LdapResource.class,
      ApiMailConfigurationResource.class,
      ApiProxyServerConfigurationResource.class,
      SystemNoticeResource.class,
      SuccessMetricsResource.class,
      AutomaticApplicationsConfigurationResource.class,
      AdvancedSearchResource.class,
      AutomaticSourceControlConfigurationResource.class,
      ApiCrowdConfigurationResourceV2.class,
      WebhookResource.class
  );

  @Override
  public boolean isBanned(Class<?> clazz) {
    return BANNED_REST_RESOURCES.stream().anyMatch(banned -> banned.isAssignableFrom(clazz));
  }
}
