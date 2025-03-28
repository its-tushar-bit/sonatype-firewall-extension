/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestOptionsDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverRequestService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Path(PublicApiPaths.POLICY_WAIVER_REQUEST_PATH)
@Tag(name = "Policy Waiver Requests", description = "Use this REST API to manage policy waiver requests.")
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_WAIVERS)
public class ApiPolicyWaiverRequestResource
{
  private final ApiPolicyWaiverRequestService apiPolicyWaiverRequestService;

  static final String OWNERS_PATH =
      "{ownerType: application|organization|repository|repository_manager|repository_container}/{ownerId}";

  static final String BY_POLICY_VIOLATION_ID_PATH = OWNERS_PATH + "/{policyViolationId}";

  @Inject
  public ApiPolicyWaiverRequestResource(ApiPolicyWaiverRequestService apiPolicyWaiverRequestService) {
    this.apiPolicyWaiverRequestService = apiPolicyWaiverRequestService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_WAIVER_REQUEST)
  @Path(BY_POLICY_VIOLATION_ID_PATH)
  @Operation(
      description = "Use this method to create a policy waiver request." +
          "\n" +
          "\n" +
          "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200", description = "The new policy waiver request.",
              useReturnTypeSchema = true
          )
      }
  )
  public ApiPolicyWaiverRequestDTO addPolicyWaiverRequestByPolicyViolationId(
      @Parameter(description = "Indicates the scope of the policy waiver request. Possible values are application, " +
          "organization, repository, repository_manager, repository_container.", required = true)
      @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Enter the id for the ownerType provided above. E.g. applicationId if the " +
          "ownerType is application.", required = true)
      @PathParam("ownerId") String ownerId,
      @Parameter(description = "Enter the policyViolationId for the policy on which you want to create a policy "
          + "waiver request. Use the Policy Violation REST API or Reports REST API to obtain the policyViolationId.")
      @PathParam("policyViolationId") String policyViolationId,
      @RequestBody(
          description = "The request JSON can include the fields" +
              "<ol>" +
              "<li>comment (optional, to indicate the reason of " +
              "the waiver) default value is null</li>" +
              "<li>matcherStrategy (enumeration, required) can have values DEFAULT, EXACT_COMPONENT, ALL_COMPONENTS, " +
              "ALL_VERSIONS. DEFAULT will match all components if no hash is provided.</li>" +
              "<li>expiryTime (default null) to set the datetime when the waiver expires.</li>" +
              "</ol>",
          required = true
      )
      ApiPolicyWaiverRequestOptionsDTO apiPolicyWaiverRequestOptionsDTO)
  {
    return apiPolicyWaiverRequestService.addPolicyWaiverRequestByPolicyViolationId(ownerType, ownerId,
        policyViolationId, apiPolicyWaiverRequestOptionsDTO);
  }
}
