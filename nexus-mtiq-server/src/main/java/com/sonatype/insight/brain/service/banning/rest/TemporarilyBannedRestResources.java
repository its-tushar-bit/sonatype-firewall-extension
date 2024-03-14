/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import java.util.List;

import com.sonatype.insight.brain.api.experimental.legal.ApiLicenseLegalResource;
import com.sonatype.insight.brain.api.v2.ApiProxyServerConfigurationResource;
import com.sonatype.insight.brain.api.v2.DefaultApiJiraConfigurationResource;
import com.sonatype.insight.brain.api.v2.ApiLegalAttributionReportTemplateResourceV2;
import com.sonatype.insight.brain.api.v2.ApiLegalReportResourceV2;
import com.sonatype.insight.brain.configuration.ldap.LdapResource;
import com.sonatype.insight.brain.ide.IdeResource;
import com.sonatype.insight.brain.labs.LabsResource;
import com.sonatype.insight.brain.service.banning.BannedImplementation;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsResource;

import com.google.common.collect.ImmutableList;

/**
 * It was determined as part of CLM-23906, CLM-23907 that certain REST resources are not going to be supported for
 * certain milestones, mostly because they relate to Lifecycle functionality and are not needed to support Firewall.
 * This class encapsulates those REST resources and is expected to change as we test and adapt each Resource to be MTIQ
 * supported.
 */
public class TemporarilyBannedRestResources
    implements BannedImplementation
{
  private static final List<Class> BANNED_REST_RESOURCES = ImmutableList.of(
      LdapResource.class,
      ApiProxyServerConfigurationResource.class,
      SuccessMetricsResource.class,

      // Integrations CLM-27720
      IdeResource.class,

      // Labs CLM-27720
      LabsResource.class,

      // Jira CLM-27720
      DefaultApiJiraConfigurationResource.class,

      // Advanced legal pack CLM-27720
      ApiLicenseLegalResource.class,
      ApiLegalAttributionReportTemplateResourceV2.class,
      ApiLegalReportResourceV2.class
  );

  @Override
  public boolean isBanned(Class<?> clazz) {
    return BANNED_REST_RESOURCES.stream().anyMatch(banned -> banned.isAssignableFrom(clazz));
  }
}
