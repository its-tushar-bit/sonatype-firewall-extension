/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

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

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dto.ApplicableContext;
import com.sonatype.insight.brain.label.LabelService.ApplicableLabels;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(LabelResource.RESOURCE_PATH)
public class LabelResource
{
  public static final String RESOURCE_PATH = "rest/label/{ownerType: application|organization|repository}/{ownerId}";

  private final LabelService labelService;

  @Inject
  public LabelResource(final LabelService labelService) {
    this.labelService = labelService;
  }

  /**
   * @param inherit boolean if {@code true} the returned list will include labels inherited from organization
   *          hierarchy, default is {@code false}
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Label> getLabels(@PathParam("ownerType") OwnerType ownerType,
                               @PathParam("ownerId") String ownerId,
                               @QueryParam("inherit") @DefaultValue("false") boolean inherit)
  {
    return labelService.getLabels(ownerType, ownerId, inherit);
  }

  /**
   * Returns all the labels associated with an ownerId. The labels are grouped by ownerId and the owner name and type
   * are returned.
   *
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("applicable")
  public ApplicableLabels getApplicableLabels(@PathParam("ownerType") OwnerType ownerType,
                                              @PathParam("ownerId") String ownerId)
  {
    return labelService.getApplicableLabels(ownerType, ownerId);
  }

  /**
   * Enumerates the contexts (org/app) in which the given label could be applied.
   *
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("applicable/context/{labelId}")
  public ApplicableContext getApplicableContexts(@PathParam("ownerType") OwnerType ownerType,
                                                 @PathParam("ownerId") String ownerIdPrivateOrPublic,
                                                 @PathParam("labelId") String labelId)
  {
    return labelService.getApplicableContexts(ownerType, ownerIdPrivateOrPublic, labelId);
  }

  /**
   * @since 1.6
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_LABEL)
  public Label addLabel(@PathParam("ownerType") OwnerType ownerType,
                        @PathParam("ownerId") String ownerId,
                        Label label)
  {
    return labelService.addLabel(ownerType, ownerId, label);
  }

  /**
   * @since 1.6
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_LABEL)
  public Label updateLabel(@PathParam("ownerType") OwnerType ownerType,
                           @PathParam("ownerId") String ownerId,
                           Label label)
  {
    return labelService.updateLabel(ownerType, ownerId, label);
  }

  /**
   * @since 1.6
   */
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
