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
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyService;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.12.0
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)
@Tag(name = "Policies",
    description = "Use this REST API to retrieve details on all existing policies in your instance of Lifecycle.")
public class ApiPolicyResourceV2
{
  private final ApiPolicyService apiPolicyService;

  @Inject
  public ApiPolicyResourceV2(final ApiPolicyService apiPolicyService) {
    this.apiPolicyService = apiPolicyService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve all existing policies." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The response contains a `policies` object which contains a list of:" +
                  "<ul>" +
                  "<li>`id` is the policyId. It can be used in the GET method for endpoint /api/v2/policyViolations " +
                  "to retrieve policy violations for the policy, and other similar operations.</li>" +
                  "<li>`name` is the name of the policy.</li>" +
                  "<li>`ownerType` is the ownerType.</li>" +
                  "<li>`ownerId` is the internal id associated with the ownerType.</li>" +
                  "<li>`threatLevel` is the threat level that is set for this policy.</li>" +
                  "<li>`policyType` indicates the type for the policy. Values can be `Security`, `License`, " +
                  "`Quality` or `Other`.</li>",
              useReturnTypeSchema = true)
      })
  public ApiPolicyListDTO getPolicies() {
    return apiPolicyService.getPolicies();
  }
}
