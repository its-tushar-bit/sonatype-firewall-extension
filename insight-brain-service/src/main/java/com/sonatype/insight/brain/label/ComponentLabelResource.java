/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.label.ComponentLabelService.AppliedLabels;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(ComponentLabelResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_LABELS)
public class ComponentLabelResource
{
  public static final String RESOURCE_PATH =
      "rest/label/component/{ownerType: application|organization|repository|repository_manager|repository_container}"
          + "/{ownerId}/{hash}";

  private final ComponentLabelService componentLabelService;

  @Inject
  public ComponentLabelResource(final ComponentLabelService componentLabelService) {
    this.componentLabelService = componentLabelService;
  }

  /**
   * Enables visualization of applied component labels. Most notably, the returned DTO holds the names of relevant
   * entities and public IDs as opposed to internal IDs to facilitate follow-up REST requests like deletion.
   *
   * @since 1.6
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public AppliedLabels getComponentLabels(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @PathParam("hash") final String hash)
  {
    return componentLabelService.getComponentLabels(ownerType, ownerId, hash);
  }

  /**
   * Assigns an existing label to a component identified by hash in a given context (org/app).
   *
   * @since 1.6
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.ASSIGN_COMPONENT_LABEL)
  public void setComponentLabel(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @PathParam("hash") final String hash,
      final Label label)
  {
    componentLabelService.setComponentLabel(ownerType, ownerId, hash, label);
  }

  /**
   * Deletes the component label given by the owning context and label id.
   *
   * @since 1.6
   */
  @DELETE
  @Path("{labelId}")
  @Audited(AuditEvent.REMOVE_COMPONENT_LABEL)
  public void deleteComponentLabel(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @PathParam("hash") final String hash,
      @PathParam("labelId") final String labelId)
  {
    componentLabelService.deleteComponentLabel(ownerType, ownerId, hash, labelId);
  }
}
