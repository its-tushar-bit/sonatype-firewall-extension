/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Dashboard tile endpoint for the Legal Obligations widget (CLM-39604 / P1.5-D-2). Returns a discriminated
 * payload selected server-side from the caller's Advanced Legal Pack entitlement and scoped-application set; see
 * {@link LegalObligationsDashboardResponse} for shape semantics.
 *
 * <p>
 * Served under {@code /rest} — this is an internal UI-backing endpoint, not part of the supported public
 * {@code /api/v2} surface. Keeping it private lets the response shape (currently a discriminated union) iterate
 * after merge without a public-API backward-compatibility commitment. See CLM-39641 review follow-ups for the
 * eventual public-API design (split ALP / non-ALP endpoints, 403 for permission-denied).
 *
 * @since 1.205
 */
@Named
@Singleton
@Timed
@Path(LegalObligationsDashboardResource.RESOURCE_PATH)
@Tag(name = "Dashboard", description = "Aggregated read-only data backing the IQ Preview dashboard tiles.")
public class LegalObligationsDashboardResource
{
  public static final String RESOURCE_PATH = "/rest/dashboard/legalObligations";

  private final LegalObligationsDashboardService legalObligationsDashboardService;

  @Inject
  public LegalObligationsDashboardResource(final LegalObligationsDashboardService legalObligationsDashboardService) {
    this.legalObligationsDashboardService = legalObligationsDashboardService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = "Returns the discriminated Legal Obligations payload for the caller. The exact shape depends "
          + "on the caller's Advanced Legal Pack entitlement and scoped-application set: ALP-licensed tenants "
          + "receive the per-license-threat-group breakdown (top 10); non-ALP tenants receive the top 4 "
          + "license-category policy violations. Callers with no scoped applications receive a "
          + "{permissionDenied: true} body so the tile can render a graceful greyed state.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Discriminated payload (see LegalObligationsDashboardResponse).",
            useReturnTypeSchema = true)
      })
  public LegalObligationsDashboardResponse getLegalObligations() {
    return legalObligationsDashboardService.getResponse();
  }
}
