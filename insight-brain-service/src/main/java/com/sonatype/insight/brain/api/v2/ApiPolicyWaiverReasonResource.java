/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverReasonDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverReasonService;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.sonatype.insight.brain.api.PublicApiPaths.POLICY_WAIVER_REASONS_PATH;

@Named
@Timed
@Path(POLICY_WAIVER_REASONS_PATH)
@Tag(
    name = "Policy Waiver Reasons",
    description = "Use this rest API to fetch available policy waiver reasons")
@Produces(MediaType.APPLICATION_JSON)
public class ApiPolicyWaiverReasonResource
{
  private final ApiPolicyWaiverReasonService apiPolicyWaiverReasonService;

  @Inject
  public ApiPolicyWaiverReasonResource(
      final ApiPolicyWaiverReasonService apiPolicyWaiverReasonService)
  {
    this.apiPolicyWaiverReasonService = apiPolicyWaiverReasonService;
  }

  @GET
  @Operation(description = "Use this method to retrieve all policy waiver reasons." +
      "\n" +
      "\n" +
      "Permissions required: None")
  @ApiResponse(responseCode = "200",
      description = "Successfully retrieved all policy waiver reasons.",
      useReturnTypeSchema = true)
  public List<ApiPolicyWaiverReasonDTO> getPolicyWaiverReasons() {
    return this.apiPolicyWaiverReasonService.getAllPolicyWaiverReasons();
  }
}
