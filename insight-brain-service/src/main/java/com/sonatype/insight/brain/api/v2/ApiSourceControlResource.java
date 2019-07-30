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
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;

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

  public static final String APP_AND_SOURCE_CONTROL_IDS = APPLICATION_ID + "/" + SOURCE_CONTROL_ID;

  private final ApiSourceControlService sourceControlService;

  @Inject
  public ApiSourceControlResource(final ApiSourceControlService apiSourceControlService) {
    this.sourceControlService = apiSourceControlService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(APPLICATION_ID)
  public ApiSourceControlDTO getSourceControl(@PathParam("applicationId") String applicationId) {
    return convert(sourceControlService.getSourceControlByApplicationId(applicationId));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_SOURCE_CONTROL)
  @Path(APPLICATION_ID)
  public ApiSourceControlDTO addSourceControl(@PathParam("applicationId") String applicationId,
      ApiSourceControlDTO sourceControl) 
  {
    return convert(sourceControlService.addSourceControl(applicationId, convert(sourceControl)));
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_SOURCE_CONTROL)
  @Path(APPLICATION_ID)
  public ApiSourceControlDTO updateSourceControl(@PathParam("applicationId") String applicationId,
      ApiSourceControlDTO sourceControl) 
  {
    return convert(sourceControlService.updateSourceControl(applicationId, convert(sourceControl)));
  }

  @DELETE
  @Path(APP_AND_SOURCE_CONTROL_IDS)
  @Audited(AuditEvent.DELETE_SOURCE_CONTROL)
  public void deleteSourceControl(@PathParam("applicationId") String applicationId,
      @PathParam("sourceControlId") String sourceControlId) 
  {
    sourceControlService.deleteSourceControl(applicationId, sourceControlId);
  }

  private ApiSourceControlDTO convert(SourceControl sourceControl) {
    ApiSourceControlDTO apiSourceControlDTO = new ApiSourceControlDTO();
    apiSourceControlDTO.id = sourceControl.getId();
    apiSourceControlDTO.applicationId = sourceControl.getApplicationId();
    apiSourceControlDTO.repositoryUrl = sourceControl.getRepositoryUrl();
    apiSourceControlDTO.token = sourceControl.getToken();
    apiSourceControlDTO.provider = sourceControl.getProvider();
    return apiSourceControlDTO;
  }

  private SourceControl convert(ApiSourceControlDTO apiSourceControlDTO) {
    SourceControl sourceControl = new SourceControl(apiSourceControlDTO.applicationId,
        apiSourceControlDTO.repositoryUrl, apiSourceControlDTO.token, apiSourceControlDTO.provider);
    sourceControl.setId(apiSourceControlDTO.id);
    return sourceControl;
  }
}
