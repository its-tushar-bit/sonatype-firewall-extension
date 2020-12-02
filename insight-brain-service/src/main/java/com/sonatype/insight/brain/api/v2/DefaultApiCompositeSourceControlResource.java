/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiCompositeSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiCompositeSourceControlService;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.79
 */
@Named
@Timed
@Path(value = PublicApiPaths.COMPOSITE_SOURCE_CONTROL_PATH_V2)
public class DefaultApiCompositeSourceControlResource implements ApiCompositeSourceControlResource
{
  private static final String OWNER_TYPE = "{ownerType:application|organization}";

  private static final String OWNER_ID = "{internalOwnerId}";

  /* paths are package private for use in tests */
  static final String BY_OWNER = OWNER_TYPE + "/" + OWNER_ID;

  private final ApiCompositeSourceControlService apiCompositeSourceControlService;

  @Inject
  public DefaultApiCompositeSourceControlResource(
      final ApiCompositeSourceControlService apiCompositeSourceControlService)
  {
    this.apiCompositeSourceControlService = apiCompositeSourceControlService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(BY_OWNER)
  public ApiCompositeSourceControlDTO getCompositeSourceControlByOwner(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId)
  {
    return apiCompositeSourceControlService.getCompositeSourceControlByOwner(ownerType, internalOwnerId);
  }
}
