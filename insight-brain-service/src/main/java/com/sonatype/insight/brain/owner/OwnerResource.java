/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dto.OwnerHierarchyDTO;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.105
 */
@Named
@Timed
@Path(OwnerResource.RESOURCE_PATH)
public class OwnerResource
{
  public static final String RESOURCE_PATH = "rest/owner/{ownerType}/{ownerId}/hierarchy";

  private final OwnerService ownerService;

  @Inject
  public OwnerResource(OwnerService ownerService) {
    this.ownerService = ownerService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public OwnerHierarchyDTO getHierarchy(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return ownerService.getHierarchy(ownerType, ownerId);
  }
}
