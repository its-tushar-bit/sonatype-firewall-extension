/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.MoveOrganizationResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiOrganizationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.organization.MoveOrganizationService;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.11.0
 */
@Named
@Timed
@Path(PublicApiPaths.ORG_RESOURCE_PATH)
public class ApiOrganizationResourceV2
{
  public static final String ORGANIZATION_ID = "{organizationId}";

  public static final String MOVE_ORGANIZATION_PATH = ORGANIZATION_ID + "/move/destination/{destinationId}";

  private final ApiOrganizationService apiOrganizationService;

  private final MoveOrganizationService moveOrganizationService;

  @Inject
  public ApiOrganizationResourceV2(
      final ApiOrganizationService apiOrganizationService,
      final MoveOrganizationService moveOrganizationService)
  {
    this.apiOrganizationService = apiOrganizationService;
    this.moveOrganizationService = moveOrganizationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiOrganizationListDTO getOrganizations(@QueryParam("organizationName") Set<String> organizationNames) {
    return apiOrganizationService.getOrganizations(organizationNames);
  }

  /**
   * @since 1.81
   */
  @GET
  @Path(ORGANIZATION_ID)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiOrganizationDTO getOrganization(@PathParam("organizationId") String organizationId) {
    return apiOrganizationService.getOrganizationById(organizationId);
  }

  /**
   * @since 1.42
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_ORGANIZATION)
  public ApiOrganizationDTO addOrganization(final ApiOrganizationDTO organizationDTO) {
    return apiOrganizationService.addOrganization(organizationDTO);
  }

  /**
   * @since 1.161
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Path(MOVE_ORGANIZATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_ORGANIZATION)
  public Response moveOrganization(
      @PathParam("organizationId") final String orgId,
      @PathParam("destinationId") final String newParentOrgId,
      @DefaultValue("false") @QueryParam("failEarlyOnError") final boolean failEarlyOnError
  )
  {
    MoveOrganizationResponseDTO moveOrganizationResponseDTO =
        moveOrganizationService.moveOrganization(orgId, newParentOrgId, failEarlyOnError);

    if (!moveOrganizationResponseDTO.errors.isEmpty()) {
      return Response.status(Status.CONFLICT)
          .entity(moveOrganizationResponseDTO)
          .build();
    }
    return Response.status(Status.OK)
        .entity(moveOrganizationResponseDTO)
        .build();
  }

  @DELETE
  @Path(ORGANIZATION_ID)
  @Audited(AuditEvent.DELETE_ORGANIZATION)
  public void deleteOrganization(@PathParam("organizationId") final String organizationId) throws IOException {
    apiOrganizationService.deleteOrganization(organizationId);
  }
}
