/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;

/**
 * Resource configuring manifest scans.
 *
 * @since 1.99
 */
@Named
@Timed
@Path(ScmOnboardingConfigResource.RESOURCE_PATH)
public class ScmOnboardingConfigResource
{
  static final String RESOURCE_PATH = PublicApiPaths.BASE_PATH + "/experimental/config/scm-onboarding";

  private final ScmOnboardingConfigService scmOnboardingConfigService;

  @Inject
  public ScmOnboardingConfigResource(final ScmOnboardingConfigService scmOnboardingConfigService) {
    this.scmOnboardingConfigService = scmOnboardingConfigService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> manifestScanConfiguration() {
    return ImmutableMap.of("scmOnboardingFeatureEnabled", scmOnboardingConfigService.isScmOnboardingEnabled());
  }
}
