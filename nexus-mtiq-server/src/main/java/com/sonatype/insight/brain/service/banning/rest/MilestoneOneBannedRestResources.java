/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import java.util.List;

import com.sonatype.insight.brain.api.v2.ApiProxyServerConfigurationResource;
import com.sonatype.insight.brain.configuration.ldap.LdapResource;
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
      LdapResource.class,
      ApiProxyServerConfigurationResource.class,
      SuccessMetricsResource.class,
      AdvancedSearchResource.class
  );

  @Override
  public boolean isBanned(Class<?> clazz) {
    return BANNED_REST_RESOURCES.stream().anyMatch(banned -> banned.isAssignableFrom(clazz));
  }
}
