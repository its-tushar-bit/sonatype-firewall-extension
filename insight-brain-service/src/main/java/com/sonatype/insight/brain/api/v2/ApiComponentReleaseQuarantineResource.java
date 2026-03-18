/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentReleasedFromQuarantineDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentReleaseQuarantineService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.78
 */
@Named
@Timed
@Path(PublicApiPaths.COMPONENT_QUARANTINE_RELEASE_PATH_V2)
@Tag(name = "Repositories",
    description = "Use this REST API to manage quarantined components.")
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
public class ApiComponentReleaseQuarantineResource
{
  private final ApiComponentReleaseQuarantineService componentReleaseQuarantineServiceV2;

  @Inject
  public ApiComponentReleaseQuarantineResource(
      final ApiComponentReleaseQuarantineService componentReleaseQuarantineServiceV2)
  {
    this.componentReleaseQuarantineServiceV2 = componentReleaseQuarantineServiceV2;
  }

  @POST
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.RELEASE_QUARANTINE)
  @Operation(description = "Use this method to release a component from quarantine by providing the `quarantineId`." +
      "\n" +
      "\n" +
      "Use the GET method of the Reports REST API to retrieve the `quarantineId` for the quarantined component." +
      "\n" +
      "\n" +
      "Permissions required: Edit IQ Elements",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains:" +
                "<ul>" +
                "<li>The quarantined component details for the component being released.</li>" +
                "<li>The quarantine and release times.</li>" +
                "<li>A list of policy violations that were waived to release the component from quarantine.</li>" +
                "<ul>",
            useReturnTypeSchema = true)
      })
  public ApiComponentReleasedFromQuarantineDTO releaseQuarantineWithoutReEval(
      @Parameter(
          description = "Enter the component `quarantineId`.") @PathParam("quarantineId") final String quarantineId,
      @RequestBody(description = "Enter a waiver comment for releasing the component from quarantine.",
          required = true) final String comment)
  {
    return componentReleaseQuarantineServiceV2.releaseQuarantineWithoutReEval(quarantineId, comment);
  }
}
