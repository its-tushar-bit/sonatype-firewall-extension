/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiDataRetentionPoliciesDTO;
import com.sonatype.insight.brain.api.v2.service.ApiDataRetentionPolicyService;

import com.codahale.metrics.annotation.Timed;

/**
 * @since version.next
 */
@Named
@Timed
@Path(PublicApiPaths.DATA_RETENTION_POLICY_RESOURCE_PATH)
public class ApiDataRetentionPolicyResource
{
  static final String ORGANIZATION_PATH = "organizations/{organizationId}";

  private final ApiDataRetentionPolicyService dataRetentionService;

  @Inject
  public ApiDataRetentionPolicyResource(ApiDataRetentionPolicyService dataRetentionService) {
    this.dataRetentionService = dataRetentionService;
  }

  @GET
  @Path(ORGANIZATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiDataRetentionPoliciesDTO getDataRetentionPolicies(@PathParam("organizationId") String organizationId) {
    return dataRetentionService.getDataRetentionPolicies(organizationId);
  }

  @PUT
  @Path(ORGANIZATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  public void setDataRetentionPolicies(
      @PathParam("organizationId") String organizationId,
      ApiDataRetentionPoliciesDTO dto)
  {
    dataRetentionService.setDataRetentionPolicies(organizationId, dto);
  }
}
