/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Firewall-specific endpoint to retrieve a single policy waiver by ID.
 *
 * <p>
 * Unlike the shared {@code /api/v2/policyWaivers} endpoint, this endpoint uses
 * {@link FirewallPermissionGate} for authorization instead of owner-level READ checks.
 * This allows scoped Firewall users (who have READ on specific proxy repositories but not
 * on the waiver's owner entity) to view waiver details for waivers visible in their
 * Firewall Dashboard waivers list.
 *
 * <p>
 * <b>Scope enforcement:</b> The resolved {@code permittedRepositoryIds} set is passed
 * to the service layer, which validates that the requested owner is reachable from the
 * user's permitted repositories before returning the waiver. Full-access users
 * ({@code permittedRepositoryIds == null}) bypass this check.
 */
@Named
@Singleton
@Timed
@Produces(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
@Path(PublicApiPaths.FIREWALL_RESOURCE_PATH + "/policyWaivers")
@Tag(name = ApiFirewallResource.SWAGGER_UI_API_LABEL)
public class ApiFirewallPolicyWaiverDetailResource
{
  static final String OWNERS_PATH =
      "{ownerType: application|organization|repository|repository_manager|repository_container}/{ownerId}";

  static final String BY_POLICY_WAIVER_ID_PATH = OWNERS_PATH + "/{policyWaiverId}";

  private final FirewallPermissionGate firewallPermissionGate;

  private final ApiPolicyWaiverService apiPolicyWaiverService;

  @Inject
  public ApiFirewallPolicyWaiverDetailResource(
      final FirewallPermissionGate firewallPermissionGate,
      final ApiPolicyWaiverService apiPolicyWaiverService)
  {
    this.firewallPermissionGate = firewallPermissionGate;
    this.apiPolicyWaiverService = apiPolicyWaiverService;
  }

  @GET
  @Path(BY_POLICY_WAIVER_ID_PATH)
  @Operation(
      description = "Retrieve a single Firewall policy waiver by ID. " +
          "<p>" +
          "This endpoint is for Firewall Dashboard use only. Authorization is based on Firewall " +
          "repository-level access rather than owner-level READ permission, allowing scoped users " +
          "to view waivers visible in their Firewall Dashboard list." +
          "<p>" +
          "For scoped users, the waiver owner must be reachable from their permitted proxy " +
          "repositories: a permitted repository itself, an organization or repository-manager " +
          "ancestor of one, or a container-image application linked to a permitted repository via " +
          "its shadow organization. Waivers at root-organization or repository-manager scope are " +
          "accessible to any authenticated user with access to at least one proxy repository." +
          "<p>" +
          "Permissions Required: READ permission on at least one proxy repository.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Waiver details"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403",
            description = "No access to any proxy repository, or waiver owner is outside the user's permitted scope"),
        @ApiResponse(responseCode = "404", description = "Waiver not found")
      })
  public ApiPolicyWaiverDTO getPolicyWaiver(
      @Parameter(
          description = "Owner type (application, organization, repository, repository_manager, or repository_container)",
          required = true) @PathParam("ownerType") OwnerType ownerType,
      @Parameter(description = "Owner ID", required = true) @PathParam("ownerId") String ownerId,
      @Parameter(description = "Policy waiver ID", required = true) @PathParam("policyWaiverId") String policyWaiverId)
  {
    Set<String> permittedRepositoryIds = firewallPermissionGate.resolvePermittedRepositoryIds();
    return apiPolicyWaiverService.getPolicyWaiverForFirewall(ownerType, ownerId, policyWaiverId,
        permittedRepositoryIds);
  }
}
