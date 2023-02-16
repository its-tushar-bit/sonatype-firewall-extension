/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

  public static final String GET_OWNER_DETAILS_PATH = "/{ownerType:application|organization}/{ownerId}/details";

  public static final String GET_GLOBAL_OWNER_DETAILS_PATH = "/{ownerType:repository_container}/details";

  private final SidebarService sidebarService;

  @Inject
  public SidebarResource(final SidebarService sidebarService) {
    this.sidebarService = sidebarService;
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
    String internalOwnerId = IdUtils.getInternalOwnerId(ownerType, ownerId);
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
    return getOwnerDetails(ownerType, IdUtils.getInternalOwnerId(ownerType, null));
  }
}
