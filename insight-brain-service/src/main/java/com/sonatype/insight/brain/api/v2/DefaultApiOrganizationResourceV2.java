/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiOrganizationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 *
 * @since 1.11.0
 */
@Named
@Timed
@Path(PublicApiPaths.ORG_RESOURCE_PATH)
public class DefaultApiOrganizationResourceV2 implements ApiOrganizationResourceV2
{
  public static final String ORGANIZATION_ID = "{organizationId}";

  private final ApiOrganizationService apiOrganizationService;

  @Inject
  public DefaultApiOrganizationResourceV2(final ApiOrganizationService apiOrganizationService) {
    this.apiOrganizationService = apiOrganizationService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiOrganizationListDTO getOrganizations(@QueryParam("organizationName") Set<String> organizationNames) {
    return apiOrganizationService.getOrganizations(organizationNames);
  }

  @Override
  @GET
  @Path(ORGANIZATION_ID)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiOrganizationDTO getOrganization(@PathParam("organizationId") String organizationId) {
    return apiOrganizationService.getOrganizationById(organizationId);
  }

  @Override
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_ORGANIZATION)
  public ApiOrganizationDTO addOrganization(final ApiOrganizationDTO organizationDTO) {
    return apiOrganizationService.addOrganization(organizationDTO);
  }
}
