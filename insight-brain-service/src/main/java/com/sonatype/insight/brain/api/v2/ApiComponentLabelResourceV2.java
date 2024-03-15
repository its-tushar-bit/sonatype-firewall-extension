/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiComponentLabelServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;

/**
 * Provides publicly available component label actions.
 *
 * @since 1.48
 */
@Path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
@Named
@Timed
public class ApiComponentLabelResourceV2
{
  private final ApiComponentLabelServiceV2 apiComponentLabelService;

  @Inject
  public ApiComponentLabelResourceV2(final ApiComponentLabelServiceV2 apiComponentLabelService) {
    this.apiComponentLabelService = apiComponentLabelService;
  }

  /**
   * Assigns an existing label to a component identified by hash in a given owner.
   */
  @POST
  @Audited(AuditEvent.ASSIGN_COMPONENT_LABEL)
  public void setComponentLabel(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") final String internalOwnerId,
      @PathParam("componentHash") final String componentHash,
      @PathParam("labelName") final String labelName)
  {
    apiComponentLabelService.setComponentLabel(ownerType, internalOwnerId, componentHash, labelName);
  }

  /**
   * Deletes the component label identified by hash in a given owner.
   */
  @DELETE
  @Audited(AuditEvent.REMOVE_COMPONENT_LABEL)
  public void deleteComponentLabel(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") final String internalOwnerId,
      @PathParam("componentHash") final String componentHash,
      @PathParam("labelName") final String labelName)
  {
    apiComponentLabelService.deleteComponentLabel(ownerType, internalOwnerId, componentHash, labelName);
  }
}
