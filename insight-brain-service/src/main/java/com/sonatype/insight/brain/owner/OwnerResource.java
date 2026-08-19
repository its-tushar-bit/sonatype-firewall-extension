/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
  public static final String RESOURCE_PATH = "rest/owner/{ownerType}/{ownerId}";

  static final String HIERARCHY_PATH = "hierarchy";

  static final String LEGAL_REVIEWER_HIERARCHY_PATH = HIERARCHY_PATH + "/legalReviewer";

  static final String DETAILS_PATH = "details";

  private final OwnerService ownerService;

  @Inject
  public OwnerResource(OwnerService ownerService) {
    this.ownerService = ownerService;
  }

  @GET
  @Path(HIERARCHY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public OwnerHierarchyDTO getHierarchy(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return ownerService.getHierarchyForRead(ownerType, ownerId);
  }

  @GET
  @Path(LEGAL_REVIEWER_HIERARCHY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public OwnerHierarchyDTO getHierarchyForLegalReviewer(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return ownerService.getHierarchyForLegalReviewer(ownerType, ownerId);
  }

  @GET
  @Path(DETAILS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public OwnerDTO getOwnerByTypeAndInternalId(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerInternalId)
  {
    return ownerService.getOwnerByTypeAndInternalId(ownerType, ownerInternalId);
  }
}
