/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(ProprietaryConfigResource.RESOURCE_PATH)
public class ProprietaryConfigResource
{
  // This path is maintained to enable forward & backward support for Nexus
  public static final String RESOURCE_PATH = "rest/config/proprietary";

  public static final String APPLICATION_PARAM = "applicationPublicId";

  public static final String GOAL_PARAM = "goal";

  private ProprietaryConfigService proprietaryConfigService;

  @Inject
  public ProprietaryConfigResource(ProprietaryConfigService proprietaryConfigService) {
    this.proprietaryConfigService = proprietaryConfigService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ProprietaryConfig get(@QueryParam(GOAL_PARAM) Goal goal,
                               @QueryParam(APPLICATION_PARAM) String applicationPublicId)
  {
    return proprietaryConfigService.getProprietaryConfig(goal, applicationPublicId);
  }
}
