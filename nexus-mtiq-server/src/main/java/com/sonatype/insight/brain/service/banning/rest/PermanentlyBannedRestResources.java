/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import java.util.List;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesResource;
import com.sonatype.insight.brain.api.v2.ApiCrowdConfigurationResourceV2;
import com.sonatype.insight.brain.api.v2.ApiDataRetentionPolicyResource;
import com.sonatype.insight.brain.api.v2.ApiExternalTelemetryResourceV2;
import com.sonatype.insight.brain.api.v2.ApiSourceControlConfigurationResource;
import com.sonatype.insight.brain.service.banning.BannedImplementation;
import com.sonatype.insight.brain.support.SupportResource;

import com.google.common.collect.ImmutableList;

/**
 * It was determined as part of CLM-23906, CLM-23907 that certain REST resources will never be applicable to MTIQ. This
 * class is responsible for excluding those REST resources.
 */
public class PermanentlyBannedRestResources
    implements BannedImplementation
{
  private static final List<Class> BANNED_REST_RESOURCES = ImmutableList.of(
      ApiCrowdConfigurationResourceV2.class,
      ApiDataRetentionPolicyResource.class,
      ApiExternalTelemetryResourceV2.class,
      ApiConfigFeaturesResource.class,
      SupportResource.class,
      ApiSourceControlConfigurationResource.class
  );

  @Override
  public boolean isBanned(Class<?> clazz) {
    return BANNED_REST_RESOURCES.stream().anyMatch(banned -> banned.isAssignableFrom(clazz));
  }
}
