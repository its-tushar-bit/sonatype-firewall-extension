/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentOrPurlIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentVersionsServiceV2;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.47
 */
@Named
@Timed
@Path(PublicApiPaths.COMPONENT_VERSIONS_PATH_V2)
public class DefaultApiComponentVersionsResourceV2 implements ApiComponentVersionsResourceV2
{
  private final ApiComponentVersionsServiceV2 componentVersionsService;

  @Inject
  public DefaultApiComponentVersionsResourceV2(final ApiComponentVersionsServiceV2 componentVersionsService) {
    this.componentVersionsService = componentVersionsService;
  }

  @Override
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> getComponentVersions(final ApiComponentOrPurlIdentifierDTOV2 componentOrPurlIdentifier) {
    return componentVersionsService.getComponentVersions(componentOrPurlIdentifier);
  }
}
