/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiStaleWaiversResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiStaleWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @since 1.81
 */
@Named
@Timed
@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiStaleWaiversReportingResource.PATH)
@Consumes(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.WAIVER_REPORTS)
public class ApiStaleWaiversReportingResource
{
  public static final String PATH = "/waivers/stale";

  private final ApiStaleWaiverService staleWaiverService;

  @Inject
  public ApiStaleWaiversReportingResource(final ApiStaleWaiverService staleWaiverService) {
    this.staleWaiverService = staleWaiverService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_STALE_WAIVERS)
  @Operation(description = "Stale waivers pose a risk because they could be applied unintentionally. " +
      "Use this method to retrieve stale waivers to eliminate this risk for future application evaluations. " +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements. You can view stale waivers only for applications/repositories " +
      " to which you have access. ",
      responses = {
          @ApiResponse(
              responseCode = "409",
              description = "Found waivers for applications/repositories that have not been evaluated since IQ " +
                  "Server version 76. Re-evaluating the repository is recommended."),
          @ApiResponse(
              responseCode = "200",
              description =
                  "The response contains waiverId of the stale waiver, policyId and policyName of the policy " +
                      "being waived, comment, waiver scope, time created, expiry time and the waiver " +
                      "creator details. " +
                      "The response field staleEvaluations contains a list of applications or repositories " +
                      "that have not " +
                      "been evaluated since the waiver was created. ",
              useReturnTypeSchema = true
          )
      }
  )
  public ApiStaleWaiversResponseDTO getStaleWaivers() {
    ApiStaleWaiversResponseDTO staleWaiversResponseDTO = new ApiStaleWaiversResponseDTO();
    staleWaiversResponseDTO.staleWaivers = staleWaiverService.getStaleWaivers();

    return staleWaiversResponseDTO;
  }
}
