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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiBulkWaiversDTO;
import com.sonatype.insight.brain.api.v2.service.ApiFirewallBulkWaiverService;
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

@Named
@Singleton
@Timed
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
@Path(PublicApiPaths.FIREWALL_RESOURCE_PATH +
    "/repositories/{ownerType:organization|repository|repository_manager|repository_container}/{ownerId}/waivers/bulk")
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL)
public class ApiFirewallBulkPolicyWaiverResource
{
  private final ApiFirewallBulkWaiverService apiFirewallBulkWaiverService;

  @Inject
  public ApiFirewallBulkPolicyWaiverResource(final ApiFirewallBulkWaiverService apiFirewallBulkWaiverService) {
    this.apiFirewallBulkWaiverService = apiFirewallBulkWaiverService;
  }

  @POST
  @Audited(AuditEvent.CREATE_WAIVER)
  @Operation(
      description = "Bulk-waive up to 1000 repository policy violations for Firewall. " +
          "Supports both quarantine violations (FAIL action) and non-quarantine violations (WARN action). " +
          "Quarantined components are released on the next monitoring or reevaluation pass once all remaining " +
          "FAIL violations have been waived." +
          "\n\n" +
          "This operation executes within a single database transaction - if any violation fails validation, " +
          "the entire request is rolled back (no partial waivers are created)." +
          "\n\n" +
          "**Performance**: Requests with 100 violations typically complete in <5 seconds, " +
          "requests with 1000 violations complete in <10 seconds." +
          "\n\n" +
          "**Permissions required**: Waive Policy Violations" +
          "\n\n" +
          "**Security**: All violations must belong to the authenticated owner's tenant hierarchy. " +
          "Cross-tenant access attempts will be rejected.",
      responses = {
        @ApiResponse(
            responseCode = "204",
            description = "Bulk waiver created successfully."),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request. Possible causes:\n" +
                "- More than 1000 violation IDs provided\n" +
                "- Violation IDs list is null or empty\n" +
                "- One or more violation IDs do not exist\n" +
                "- One or more violations do not belong to the specified owner\n" +
                "- Waiver options are invalid (e.g., expiry date in past, blank comment)\n" +
                "- Unsupported matcher strategy (only EXACT_COMPONENT and ALL_VERSIONS are supported)\n" +
                "- expireWhenRemediationAvailable=true with matcherStrategy=ALL_VERSIONS"),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden. Either FIREWALL license is not active or user lacks WAIVE_POLICY_VIOLATIONS permission."),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found. Repository associated with a violation does not exist.")
      })
  public void addBulkWaivers(
      @Parameter(
          description = "Owner type scope for the waiver. Must be one of: organization, repository, repository_manager, repository_container. "
              +
              "Use 'organization' with ownerId 'ROOT_ORGANIZATION_ID' to apply waiver across all repositories.",
          required = true,
          example = "repository") @PathParam("ownerType") final com.sonatype.insight.brain.model.OwnerType ownerType,
      @Parameter(
          description = "Owner ID corresponding to the owner type. All violations must belong to this owner's tenant hierarchy.",
          required = true,
          example = "repo-12345") @PathParam("ownerId") final String ownerId,
      @RequestBody(
          description = "Bulk waiver request containing:\n" +
              "- **violationIds** (required, 1-1000 items): List of repository policy violation IDs to waive. " +
              "Duplicate IDs are automatically deduplicated. Supports both quarantine (FAIL) and non-quarantine (WARN) violations. "
              +
              "Already-waived violations are skipped without error.\n" +
              "- **apiWaiverOptionsDTO** (required): Waiver options including:\n" +
              "  - **comment** (required, non-blank): Reason for waiving these violations\n" +
              "  - **matcherStrategy** (required): EXACT_COMPONENT or ALL_VERSIONS\n" +
              "  - **expiryTime** (optional): Must be in the future if provided\n" +
              "  - **waiverReasonId** (optional): Reference to a pre-defined waiver reason\n" +
              "  - **expireWhenRemediationAvailable** (optional): If true, matcherStrategy must be EXACT_COMPONENT",
          required = true) final ApiBulkWaiversDTO bulkWaiversDTO)
  {
    apiFirewallBulkWaiverService.addBulkPolicyWaivers(ownerType, ownerId, bulkWaiversDTO);
  }
}
