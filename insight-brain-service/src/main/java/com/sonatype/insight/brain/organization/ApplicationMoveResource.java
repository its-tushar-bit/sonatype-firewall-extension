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
 * Supports moving an application to another parent organization.
 * 
 * @since 1.20
 */
@Named
@Timed
@Path(ApplicationMoveResource.RESOURCE_PATH)
public class ApplicationMoveResource
{
  static final String RESOURCE_PATH = "rest/move/application/{applicationId}";

  static final String DESTINATIONS_PATH = "destinations";

  private final ApplicationMoveService applicationMoveService;

  @Inject
  public ApplicationMoveResource(ApplicationMoveService applicationMoveService) {
    this.applicationMoveService = applicationMoveService;
  }

  @GET
  @Path(DESTINATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<Organization> getDestinationOrganizations(@PathParam("applicationId") String applicationId) {
    return applicationMoveService.getDestinationOrganizations(applicationId);
  }
}
