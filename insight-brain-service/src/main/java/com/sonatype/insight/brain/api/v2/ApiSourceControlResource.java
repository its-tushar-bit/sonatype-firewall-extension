/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.66
 */
@Named
@Timed
@Path(value = PublicApiPaths.SOURCE_CONTROL_PATH_V2)
public class ApiSourceControlResource
{
  private static final String SOURCE_CONTROL_ID = "{sourceControlId}";

  private static final String APPLICATION_ID = "{applicationId}";

  private static final String APP_AND_SOURCE_CONTROL_IDS = APPLICATION_ID + "/" + SOURCE_CONTROL_ID;

  private final ApiSourceControlService sourceControlService;

  private final ApiSourceControlAdapter apiSourceControlAdapter;

  @Inject
  public ApiSourceControlResource(
      final ApiSourceControlService apiSourceControlService,
      final ApiSourceControlAdapter apiSourceControlAdapter)
  {
    this.sourceControlService = apiSourceControlService;
    this.apiSourceControlAdapter = apiSourceControlAdapter;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(APPLICATION_ID)
  public ApiSourceControlDTO getSourceControl(@PathParam("applicationId") String applicationId) {
    return apiSourceControlAdapter.convertToDTO(
        sourceControlService.getSourceControlByApplicationId(applicationId));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_SOURCE_CONTROL)
  @Path(APPLICATION_ID)
  public ApiSourceControlDTO addSourceControl(@PathParam("applicationId") String applicationId,
      ApiSourceControlDTO sourceControl) 
  {
    return apiSourceControlAdapter.convertToDTO(
        sourceControlService.addSourceControl(
            applicationId,
            apiSourceControlAdapter.convertFromDTO(sourceControl)));
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_SOURCE_CONTROL)
  @Path(APPLICATION_ID)
  public ApiSourceControlDTO updateSourceControl(@PathParam("applicationId") String applicationId,
      ApiSourceControlDTO sourceControl) 
  {
    return apiSourceControlAdapter.convertToDTO(
        sourceControlService.updateSourceControl(
            applicationId,
            apiSourceControlAdapter.convertFromDTO(sourceControl)));
  }

  @DELETE
  @Path(APP_AND_SOURCE_CONTROL_IDS)
  @Audited(AuditEvent.DELETE_SOURCE_CONTROL)
  public void deleteSourceControl(@PathParam("applicationId") String applicationId,
      @PathParam("sourceControlId") String sourceControlId) 
  {
    sourceControlService.deleteSourceControl(applicationId, sourceControlId);
  }
}
