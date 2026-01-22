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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentWaiversDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentsWithWaiversReportingService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @since 1.76
 */
@Named
@Timed
@Path(PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsWithWaiversReportingResource.PATH)
@Consumes(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.WAIVER_REPORTS)
public class ApiComponentsWithWaiversReportingResource
{
  public static final String PATH = "/components/waivers";

  private final ApiComponentsWithWaiversReportingService componentsWithWaiversReportingService;

  @Inject
  public ApiComponentsWithWaiversReportingResource(
      ApiComponentsWithWaiversReportingService componentsWithWaiversReportingService)
  {
    this.componentsWithWaiversReportingService = componentsWithWaiversReportingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENTS_WITH_WAIVERS)
  @Operation(description = "Use this method to retrieve existing policy waivers by components. For an up-to-date " +
      "response, ensure that all application and repository reports are current and contain the most recent " +
      "re-evaluation data." +
      "<p>" +
      "You can specify the format/ecosystem of the component for a filtered result. " +
      "<p>" +
      "Permissions required: View IQ Elements and access to the specific applications and repositories ",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The JSON response contains waivers grouped by application components and repository " +
                  "components. Waived violations for application components are listed per stage. " +
                  "Waived violations for repository components are listed in the Proxy stage. " +
                  "<p>" +
                  "The component hash is null if the waiver applies to all components or all versions " +
                  "of a component. It is truncated and meant to be used as an identifier to other REST API calls " +
                  "and not for use as checksum. " +
                  "<p>" +
                  "`isObsolete` indicates if a waived violation does not have a valid waiver information. " +
                  "This could happen when a waiver has been removed and the report has not been re-evaluated." +
                  "<p>" +
                  "`matcherStrategy` can have values EXACT_COMPONENT, ALL_COMPONENTS, ALL_VERSIONS. " +
                  "<p>" +
                  "The response fields `associatedPackageUrl`, `componentIdentifier` and `displayName` " +
                  "are returned only if the waiver is of type ALL_VERSIONS OR EXACT_COMPONENTS " +
                  "and the component is not an unknown component .",
              useReturnTypeSchema = true

          )
      }
  )
  public ApiComponentWaiversDTO getComponentsWithWaivers(
      @Parameter(description = "Enter the format/ecosystem of the component")
      @QueryParam("format") String format)
  {
    return componentsWithWaiversReportingService.getComponentsWithWaivers(format);
  }
}
