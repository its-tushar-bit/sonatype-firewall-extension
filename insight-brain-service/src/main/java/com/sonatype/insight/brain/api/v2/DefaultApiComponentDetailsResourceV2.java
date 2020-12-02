/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsResultDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsServiceV2;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.16.0
 */
@Named
@Timed
@Path(PublicApiPaths.COMPONENT_DETAILS_PATH_V2)
public class DefaultApiComponentDetailsResourceV2 implements ApiComponentDetailsResourceV2
{
  private final ApiComponentDetailsServiceV2 componentDetailsService;

  @Inject
  public DefaultApiComponentDetailsResourceV2(final ApiComponentDetailsServiceV2 componentDetailsService) {
    this.componentDetailsService = componentDetailsService;
  }

  @Override
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiComponentDetailsResultDTOV2 getComponentDetails(ApiComponentDetailsRequestDTOV2 componentDetailsRequest) {
    return componentDetailsService.getComponentDetails(componentDetailsRequest);
  }
}
