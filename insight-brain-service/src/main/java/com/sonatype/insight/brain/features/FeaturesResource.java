/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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

@Named
@Path(FeaturesResource.SERVICE_PATH)
public class FeaturesResource
{
  public static final String SERVICE_PATH = "rest/features";

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
}
