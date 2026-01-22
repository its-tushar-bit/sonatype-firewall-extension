/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.utils.IdUtils;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.18.0
 */
@Named
@Timed
@Path(SidebarResource.RESOURCE_PATH)
public class SidebarResource
{
  public static final String RESOURCE_PATH = "rest/sidebar";

  public static final String GET_OWNER_DETAILS_PATH =
      "/{ownerType:application|organization|repository_manager|repository}/{ownerId}/details";

  public static final String GET_GLOBAL_OWNER_DETAILS_PATH = "/{ownerType:repository_container}/details";

  private final SidebarService sidebarService;

  private final IdUtils idUtils;

  @Inject
  public SidebarResource(final SidebarService sidebarService, final IdUtils idUtils) {
    this.sidebarService = sidebarService;
    this.idUtils = idUtils;
  }

  /**
   * Returns the owner hierarchy for use in the Owner Tree View
   *
   * @since 1.20.0
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public OwnerHierarchyDTO getOwnerList() {
    return sidebarService.getOwnerList();
  }

  /**
   * Returns a owner's entities that can be managed by the Owner Management UI.
   *
   * @since 1.18.0
   */
  @GET
  @Path(GET_OWNER_DETAILS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public OwnerDetailsDTO getOwnerDetails(@PathParam("ownerType") final OwnerType ownerType,
                                         @PathParam("ownerId") final String ownerId)
  {
    String internalOwnerId = idUtils.getInternalOwnerId(ownerType, ownerId);
    return sidebarService.getOwnerDetails(ownerType, internalOwnerId);
  }

  /**
   * Returns a owner's entities that can be managed by the Owner Management UI.
   *
   * @since 1.20.0
   */
  @GET
  @Path(GET_GLOBAL_OWNER_DETAILS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public OwnerDetailsDTO getGlobalOwnerDetails(@PathParam("ownerType") final OwnerType ownerType) {
    return getOwnerDetails(ownerType, idUtils.getInternalOwnerId(ownerType, null));
  }
}
