/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiComponentLabelServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Provides publicly available component label actions.
 *
 * @since 1.48
 */
@Path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
@Named
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_LABELS)
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
  @Operation(description = "Use this method to assign an existing label to a component.")
  @ApiResponse(responseCode = "204", description = "Component label assigned successfully.")
  public void setComponentLabel(
      @Parameter(description = "Possible values: application or organization")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Possible values : applicationId or organizationId")
      @PathParam("internalOwnerId") final String internalOwnerId,
      @Parameter(description = "Enter the SHA1 hash of the component.")
      @PathParam("componentHash") final String componentHash,
      @Parameter(description = "Enter the label name to assign to this component.")
      @PathParam("labelName") final String labelName)
  {
    apiComponentLabelService.setComponentLabel(ownerType, internalOwnerId, componentHash, labelName);
  }

  /**
   * Deletes the component label identified by hash in a given owner.
   */
  @DELETE
  @Audited(AuditEvent.REMOVE_COMPONENT_LABEL)
  @Operation(description = "Use this method to un-assign a label from a component.")
  @ApiResponse(responseCode = "204", description = "Label un-assigned from component successfully.")
  public void deleteComponentLabel(
      @Parameter(description = "Possible values: application or organization")
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Possible values : applicationId or organizationId")
      @PathParam("internalOwnerId") final String internalOwnerId,
      @Parameter(description = "Enter the SHA1 hash of the component.")
      @PathParam("componentHash") final String componentHash,
      @Parameter(description = "Enter the label name to un-assign from this component.")
      @PathParam("labelName") final String labelName)
  {
    apiComponentLabelService.deleteComponentLabel(ownerType, internalOwnerId, componentHash, labelName);
  }
}
