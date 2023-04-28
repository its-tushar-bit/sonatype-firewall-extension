/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.Organization;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.160
 */
@Named
@Timed
@Path(OrganizationMoveResource.RESOURCE_PATH)
public class OrganizationMoveResource
{
  static final String RESOURCE_PATH = "rest/move/organization/{organizationId}";

  static final String DESTINATIONS_PATH = "destinations";

  private final MoveOrganizationService moveOrganizationService;

  @Inject
  public OrganizationMoveResource(MoveOrganizationService moveOrganizationService) {
    this.moveOrganizationService = moveOrganizationService;
  }

  @GET
  @Path(DESTINATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<Organization> getDestinationOrganizations(@PathParam("organizationId") String organizationId) {
    return moveOrganizationService.getDestinationOrganizations(organizationId);
  }
}
