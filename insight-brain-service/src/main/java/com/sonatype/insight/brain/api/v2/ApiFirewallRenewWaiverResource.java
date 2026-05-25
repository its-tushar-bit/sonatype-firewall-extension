/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.firewall.RenewWaiversRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.firewall.RenewWaiversResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiFirewallRenewWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API resource for renewing Firewall policy waivers.
 * <p>
 * Supports both single and bulk waiver renewals through a unified endpoint.
 *
 * @since 1.186
 */
@Named
@Singleton
@Timed
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
@Path(PublicApiPaths.FIREWALL_RESOURCE_PATH + "/waivers/renew")
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL)
public class ApiFirewallRenewWaiverResource
{
  private final ApiFirewallRenewWaiverService apiFirewallRenewWaiverService;

  @Inject
  public ApiFirewallRenewWaiverResource(final ApiFirewallRenewWaiverService apiFirewallRenewWaiverService) {
    this.apiFirewallRenewWaiverService = apiFirewallRenewWaiverService;
  }

  @POST
  @Audited(AuditEvent.RENEW_WAIVER)
  @Operation(
      description = "Renews Firewall policy waivers with a new expiry date. " +
          "Records the previous expiry time for audit and tracking purposes." +
          "\\n\\n" +
          "**Supports both single and bulk renewals**: Pass one or more waiver IDs in the request body." +
          "\\n\\n" +
          "**Permissions required**: Waive Policy Violations" +
          "\\n\\n" +
          "**Security**: Permission is checked against the waiver's owner. " +
          "The authenticated user must have WAIVE_POLICY_VIOLATIONS on the owner scope.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Waiver renewal processed. Returns counts of renewed waivers, " +
                "not found waivers, and any errors."),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request. Waiver IDs list is null or empty."),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden. Either FIREWALL license is not active or user lacks WAIVE_POLICY_VIOLATIONS permission."),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found. FIREWALL_WAIVER_DASHBOARD_AND_RENEW feature is disabled.")
      })
  public Response renewWaivers(
      @RequestBody(
          description = "Renewal request containing:\\n" +
              "- **waiverIds** (required): List of waiver IDs to renew\\n" +
              "- **newExpiryTime** (optional): New expiry time as ISO 8601 timestamp. Null means the waiver never expires.\\n"
              +
              "- **comment** (optional): Comment explaining the renewal\\n" +
              "- **reasonId** (optional): ID of a pre-defined waiver reason",
          required = true) final RenewWaiversRequestDTO request)
  {
    if (!SystemConfigurationPropertyFeature.FIREWALL_WAIVER_DASHBOARD_AND_RENEW.isEnabled()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    if (request == null) {
      throw new BadRequestException("Request cannot be null");
    }
    if (request.waiverIds == null || request.waiverIds.isEmpty()) {
      throw new BadRequestException("Waiver IDs list cannot be null or empty");
    }

    RenewWaiversResponseDTO response = apiFirewallRenewWaiverService.renewWaivers(
        request.waiverIds,
        request.newExpiryTime,
        request.comment,
        request.reasonId);

    return Response.ok(response).build();
  }
}
