/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.Feature;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(FeaturesResource.RESOURCE_PATH)
@UnlicensedPath
public class FeaturesResource
{
  public static final String RESOURCE_PATH = "rest/product/features";

  public static final String NO_AUTH_VULNERABILITY_LOOKUP_PATH = "/noAuthVulnerabilityLookup";

  private final FeaturesService featuresService;

  @Inject
  public FeaturesResource(FeaturesService featuresSerive) {
    this.featuresService = featuresSerive;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Set<Feature> getFeatures() {
    return featuresService.getFeatures();
  }

  /**
   * This endpoint returns whether or not vulnerability lookup without authentication feature is enabled. It
   * needs to be accessible before the user is logged in (in contrast to `getFeatures` above) since we need to decide
   * whether or not to show the link to Vulnerability Lookup page in the login modal.
   * <p>
   * This endpoint re-uses the `featuresService.getFeatures()` but removes any features from the response that are not
   * NO_AUTH_VULNERABILITY_LOOKUP.
   * <p>
   * In other words this endpoint returns <code>[ "no-auth-vulnerability-lookup" ]</code> if vulnerability lookup is
   * allowed without authentication, otherwise it returns an empty list.
   * <p>
   * This endpoint does not return any information about any other features.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(NO_AUTH_VULNERABILITY_LOOKUP_PATH)
  public Set<Feature> getNoAuthVulnerabilityLookup() {
    Set<Feature> features = featuresService.getFeatures();
    features.removeIf(feature -> feature != InsightConfig.Feature.NO_AUTH_VULNERABILITY_LOOKUP);
    return features;
  }
}
