/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.sonatype.insight.brain.api.PublicApiPaths.CPE_MATCHING_CONFIGURATION_RESOURCE_PATH;

/**
 * @since 1.191
 */
@Named
@Timed
@Path(CPE_MATCHING_CONFIGURATION_RESOURCE_PATH)
@Tag(name = "CPE Matching Configuration",
    description = "Use the CPE Matching Configuration REST API to add/set/remove cpe matching configuration to" +
        "organizations and applications")
@Hidden
@ProductLicenseEnforcementPoint(LicensedFeature.CPE_MATCHING)
public class CpeMatchingConfigurationResource
{
  private CpeMatchingConfigurationService cpeMatchingConfigurationService;

  @Inject
  public CpeMatchingConfigurationResource(CpeMatchingConfigurationService cpeMatchingConfigurationService) {
    this.cpeMatchingConfigurationService = cpeMatchingConfigurationService;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_CPE_MATCHING_CONFIGURATION)
  @Operation(
      hidden = true,
      description = "Use this method to apply a given cpe matching configuration to an organization or application." +
          "<p>" +
          "Permissions Required: Edit IQ Elements"
  )
  @ApiResponse(
      responseCode = "200",
      description = "An object containing the cpe configuration applied to the given ownerId",
      content = @Content(mediaType = "application/json")
  )
  public CpeMatchingConfigurationDTO updateCpeMatchingConfiguration(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("internalOwnerId") String internalOwnerId,
      CpeMatchingConfigurationRequest configRequest
  )
  {
    AuditData.get().setData("enabled", configRequest == null ? null : configRequest.enabled);
    return cpeMatchingConfigurationService.updateCpeMatchingConfiguration(ownerType, internalOwnerId, configRequest);
  }
}
