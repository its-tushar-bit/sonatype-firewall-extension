/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

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

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.label.LabelService;
import com.sonatype.insight.brain.label.ApplicableLabels;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.90
 */
@Named
@Timed
@Path(PublicApiPaths.LABEL_RESOURCE_PATH)
public class DefaultApiLabelResource implements ApiLabelResource
{
  private final LabelService labelService;

  @Inject
  public DefaultApiLabelResource(final LabelService labelService) {
    this.labelService = labelService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiLabelDTO> getLabels(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @QueryParam("inherit") @DefaultValue("false") boolean inherit)
  {
    return labelService.getLabels(ownerType, ownerId, inherit);
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("applicable")
  public ApplicableLabels getApplicableLabels(@PathParam("ownerType") OwnerType ownerType,
                                              @PathParam("ownerId") String ownerId)
  {
    return labelService.getApplicableLabels(ownerType, ownerId);
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("applicable/context/{labelId}")
  public ApplicableContext getApplicableContexts(@PathParam("ownerType") OwnerType ownerType,
                                                 @PathParam("ownerId") String ownerId,
                                                 @PathParam("labelId") String labelId)
  {
    return labelService.getApplicableContexts(ownerType, ownerId, labelId);
  }

  @Override
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_LABEL)
  public ApiLabelDTO addLabel(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      ApiLabelDTO labelDTO)
  {
    return labelService.addLabel(ownerType, ownerId, labelDTO);
  }

  @Override
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_LABEL)
  public ApiLabelDTO updateLabel(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      ApiLabelDTO labelDTO)
  {
    return labelService.updateLabel(ownerType, ownerId, labelDTO);
  }

  @Override
  @DELETE
  @Path("{labelId}")
  @Audited(AuditEvent.DELETE_LABEL)
  public void deleteLabel(@PathParam("ownerType") OwnerType ownerType,
                          @PathParam("ownerId") String ownerId,
                          @PathParam("labelId") String labelId)
  {
    labelService.deleteLabel(ownerType, ownerId, labelId);
  }
}
