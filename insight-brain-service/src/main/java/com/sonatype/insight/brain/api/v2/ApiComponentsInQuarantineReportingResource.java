/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentsInQuarantineDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentsInQuarantineReportingService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @since 1.77
 */
@Named
@Timed
@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsInQuarantineReportingResource.PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
public class ApiComponentsInQuarantineReportingResource
{
  public static final String PATH = "/components/quarantined";

  private final ApiComponentsInQuarantineReportingService service;

  @Inject
  public ApiComponentsInQuarantineReportingResource(final ApiComponentsInQuarantineReportingService service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_QUARANTINED_COMPONENTS)
  @Operation(description = "Use this method to retrieve all repository components that are quarantined. " +
      "The response contains violation details and the quarantine Id of the component. Use the quarantine Id,  " +
      "to release the component from quarantine, using the Release from Quarantine REST API. " +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements and access to the specific repository.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The JSON response returns the component details and policy violation details that are " +
                  "triggering the quarantine. If a quarantined component does not show any policy violation," +
                  " it implies that " +
                  "the policy violations have been waived, but the component has not been released from quarantine. ",
              useReturnTypeSchema = true)
      })
  public ApiComponentsInQuarantineDTO getComponentsInQuarantine() {
    return service.getComponentsInQuarantine();
  }
}
