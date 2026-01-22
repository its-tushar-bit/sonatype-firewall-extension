/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.license.model.Feature;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(FeaturesResource.RESOURCE_PATH)
@UnlicensedPath
public class FeaturesResource
{
  public static final String RESOURCE_PATH = "rest/product/features";

  public static final String ENABLE_UNAUTHENTICATED_PAGES = "/enableUnauthenticatedPages";

  public static final String ENABLE_SSO_ONLY = "/enableSsoOnly";

  public static final String OAUTH2_ENABLED = "/oauth2Enabled";

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
   * This endpoint returns whether or not pages like vulnerability lookup are allowed to be accessible without
   * authentication. It needs to be accessible before the user is logged in (in contrast to `getFeatures` above) since
   * we may want to show links to the pages in places such as Login Modal where user is not logged in yet.
   * <p>
   * This endpoint re-uses the `featuresService.getFeatures()` but removes any features from the response that are not
   * ENABLE_UNAUTHENTICATED_PAGES.
   * <p>
   * In other words this endpoint returns <code>[ "enable-unauthenticated-pages" ]</code> if pages are
   * allowed without authentication, otherwise it returns an empty list.
   * <p>
   * This endpoint does not return any information about any other features.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(ENABLE_UNAUTHENTICATED_PAGES)
  public Set<Feature> getEnableUnauthenticatedPages() {
    Set<Feature> features = featuresService.getFeatures();
    features.removeIf(feature -> feature != SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES);
    return features;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(ENABLE_SSO_ONLY)
  public Set<Feature> getEnableSsoOnly() {
    Set<Feature> features = featuresService.getFeatures();
    features.removeIf(feature -> feature != SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY);
    return features;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(OAUTH2_ENABLED)
  public Set<Feature> getOAuth2Enabled() {
    Set<Feature> features = featuresService.getFeatures();
    features.removeIf(feature -> feature != SystemConfigurationPropertyFeature.OAUTH2_ENABLED);
    return features;
  }
}
